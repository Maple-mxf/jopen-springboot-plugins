package io.jopen.springboot.plugin.mongo.repository;

import com.mongodb.client.result.UpdateResult;
import io.jopen.springboot.plugin.mongo.template.builder.AggregationBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author maxuefeng
 * @since 2020/2/9
 */
@NoRepositoryBean
public class BaseRepositoryImpl<T, ID extends Serializable>
        extends SimpleMongoRepository<T, ID>
        implements BaseRepository<T, ID> {

    private final MongoOperations mongoOperations;

    private final MongoEntityInformation<T, ID> entityInformation;

    private List<Field> entityField;

    public MongoEntityInformation<T, ID> getEntityInformation() {
        return this.entityInformation;
    }

    public BaseRepositoryImpl(MongoEntityInformation<T, ID> metadata,
                              MongoOperations mongoOperations) {
        super(metadata, mongoOperations);
        this.mongoOperations = mongoOperations;
        this.entityInformation = metadata;
    }

    @Override
    public <S extends T> Optional<S> findOne(Query query) {
        return (Optional<S>) Optional.ofNullable(mongoOperations.findOne(query, entityInformation.getJavaType()));
    }

    @Override
    public <S extends T> S getOne(Query query) {
        return (S) this.findOne(query).orElse(null);
    }

    @Override
    public <S extends T> List<S> list(Query query) {
        List<T> ts = mongoOperations.find(query, entityInformation.getJavaType());
        return (List<S>) ts;
    }

    @Override
    public <S extends T> List<S> listSort(Query query, Sort sort) {
        query.with(sort);
        return this.list(query);
    }

    @Override
    public <S extends T> boolean exists(Query query) {
        return this.mongoOperations.exists(query, entityInformation.getJavaType());
    }

    @Override
    public <S extends T> long count(Query query) {
        return this.mongoOperations.count(query, entityInformation.getJavaType());
    }

    @Override
    public <S extends T> Page<S> page(Query query, Pageable pageable) {
        query.with(pageable);
        List<T> result = mongoOperations.find(query, entityInformation.getJavaType());
        Page<T> page = PageableExecutionUtils.getPage(result, pageable,
                () -> mongoOperations.count(Query.of(query).limit(-1).skip(-1),
                        entityInformation.getCollectionName()));

        return (Page<S>) page;
    }

    /**
     * @see AggregationBuilder
     */
    public List<Map> groupSum(String sumField, String... groupFields) {
        GroupOperation groupOperation = Aggregation.group(groupFields).sum(sumField).as(sumField);
        Aggregation aggregation = Aggregation.newAggregation(groupOperation);
        return mongoOperations.aggregate(aggregation, this.entityInformation.getJavaType(), Map.class)
                .getMappedResults();
    }

    /**
     * @param sumField
     * @param groupFields
     * @return
     * @see org.springframework.data.mongodb.core.query.Query
     * @see io.jopen.springboot.plugin.mongo.template.builder.QueryBuilder
     */
    @Override
    public List<Map> groupSumBy(Criteria criteria, String sumField, String... groupFields) {

        MatchOperation matchOperation = Aggregation.match(criteria);

        GroupOperation groupOperation = Aggregation.group(groupFields).sum(sumField).as(sumField);
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
        return mongoOperations.aggregate(aggregation, this.entityInformation.getJavaType(), Map.class)
                .getMappedResults();
    }

    @Override
    public MapReduceResults<T> mapReduce(String mapFunction, String reduceFunction) {
        return this.mongoOperations.mapReduce(this.entityInformation.getCollectionName(),
                mapFunction,
                reduceFunction,
                this.entityInformation.getJavaType());
    }

    @Override
    public String ensureIndex(Index index) {
        return this.mongoOperations
                .indexOps(this.entityInformation.getJavaType())
                .ensureIndex(index);
    }

    @Override
    public List<IndexInfo> getIndexInfo() {
        return this.mongoOperations.indexOps(this.entityInformation.getJavaType()).getIndexInfo();
    }

    @Override
    public <S extends T> UpdateResult update(S entity) {
        ID id = this.entityInformation.getId(entity);
        if (id == null) throw new IllegalArgumentException("BaseRepository update method id param is require");

        Query query = new Query();
        query.addCriteria(Criteria.where(entityInformation.getIdAttribute()).is(id));

        Update update = new Update();
        List<Field> fields = getEntityField();

        fields.forEach(field -> {
            try {
                Object value = field.get(entity);
                if (value != null) update.set(getDBFieldName(field), value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        });
        return mongoOperations.updateFirst(query, update, entityInformation.getJavaType());
    }

    private List<Field> getEntityField() {
        if (this.entityField == null) {
            synchronized (this) {
                this.entityField = getFields(this.entityInformation.getJavaType());
            }
        }
        return this.entityField;
    }

    private List<Field> getFields(@NonNull Class<?> type) {
        List<Field> fieldList = new ArrayList<>();
        for (; type != Object.class; type = type.getSuperclass()) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod)) continue;
                Transient annotation = field.getDeclaredAnnotation(Transient.class);
                if (annotation != null) continue;
                field.setAccessible(true);
                fieldList.add(field);
            }
        }
        return fieldList;
    }

    private String getDBFieldName(Field field) {
        org.springframework.data.mongodb.core.mapping.Field annotation
                = field.getDeclaredAnnotation(org.springframework.data.mongodb.core.mapping.Field.class);
        if (annotation != null) return annotation.value();
        return field.getName();
    }
}
