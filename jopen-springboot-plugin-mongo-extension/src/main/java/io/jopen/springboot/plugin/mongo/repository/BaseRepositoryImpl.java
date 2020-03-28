package io.jopen.springboot.plugin.mongo.repository;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.jopen.springboot.plugin.mongo.template.builder.AggregationBuilder;
import org.bson.Document;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MappedDocument;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.event.MongoMappingEvent;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * @author maxuefeng
 * @see org.springframework.data.mongodb.core.MongoTemplate
 * @since 2020/2/9
 */
@NoRepositoryBean
public class BaseRepositoryImpl<T, ID extends Serializable>
        extends SimpleMongoRepository<T, ID>
        implements BaseRepository<T, ID> {

    private final MongoOperations mongoOperations;

    private final MongoEntityInformation<T, ID> entityInformation;

    private final MongoConverter mongoConverter;

    private final EntityOperations operations;

    private final ConversionService conversionService;

    public MongoEntityInformation<T, ID> getEntityInformation() {
        return this.entityInformation;
    }

    public BaseRepositoryImpl(MongoEntityInformation<T, ID> metadata,
                              MongoOperations mongoOperations) {
        super(metadata, mongoOperations);
        this.mongoOperations = mongoOperations;
        this.entityInformation = metadata;
        this.mongoConverter = mongoOperations.getConverter();
        this.conversionService = mongoConverter.getConversionService();
        this.operations = new EntityOperations(this.mongoConverter.getMappingContext());
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

    /**
     * 若当前对应的实体类为{@link org.springframework.data.annotation.Version}标识的字段
     * 则需要使用乐观锁进行更新
     * <p>
     * 此方法不需要考虑event的发布
     *
     * @see org.springframework.data.mongodb.core.MongoTemplate#updateFirst(Query, Update, Class)
     * @see org.springframework.data.mongodb.core.MongoTemplate#doUpdate(String, Query, UpdateDefinition, Class, boolean, boolean)
     * @see org.springframework.data.mongodb.core.MongoTemplate#increaseVersionForUpdateIfNecessary(MongoPersistentEntity, UpdateDefinition)
     * @see MongoMappingEvent
     * @see org.springframework.data.annotation.Version
     * @see org.springframework.data.mongodb.core.MongoTemplate#save(Object)
     */
    @Override
    public <S extends T> UpdateResult update(S entity) {
        ID id = this.entityInformation.getId(entity);
        if (id == null) throw new IllegalArgumentException("BaseRepository update method id param is require");

        // 是否是一个新的对象
        EntityOperations.AdaptibleEntity<S> source = operations.forEntity(entity, conversionService);
        Query query = source.getByIdQuery();
        Update update = new Update();

        MappedDocument mapped = source.toMappedDocument(this.mongoConverter);
        Document dbDoc = mapped.getDocument();

        // 移除_class字段和_id字段，没有必要设置在Update中
        dbDoc.remove("_class");
        dbDoc.remove("_id");

        dbDoc.forEach((dbFieldName, updateValue) -> {
            if (updateValue != null) {
                update.set(dbFieldName, updateValue);
            }
        });

        return mongoOperations.updateFirst(query, update, entityInformation.getJavaType());
    }

    @Override
    public <S extends T> UpdateResult updateByIdAndVersion(S entity) {
        ID id = this.entityInformation.getId(entity);
        if (id == null) throw new IllegalArgumentException("BaseRepository update method id param is require");

        // 是否是一个新的对象
        EntityOperations.AdaptibleEntity<S> source = operations.forEntity(entity, conversionService);

        // 是否属于版本控制的数据 基于CAS乐观锁进行控制
        Query query;
        if (source.isVersionedEntity()) {
            if (source.getVersion() == null) throw new IllegalArgumentException("Entity version field must be setup");
            // 此处不考虑version为空的情况 Spring data会自动处理null
            query = source.getQueryForVersion();
            // 此处没有必要手动增加版本号 spring已经提供了相应的操作
            source.incrementVersion();
        } else {
            query = source.getByIdQuery();
        }

        Update update = new Update();

        MappedDocument mapped = source.toMappedDocument(this.mongoConverter);
        Document dbDoc = mapped.getDocument();

        // 移除_class字段和_id字段，没有必要设置在Update中
        dbDoc.remove("_class");
        dbDoc.remove("_id");

        if (dbDoc.size() == 0) return UpdateResult.unacknowledged();
        dbDoc.forEach((dbFieldName, updateValue) -> {
            if (updateValue != null) {
                update.set(dbFieldName, updateValue);
            }
        });

        return mongoOperations.updateFirst(query, update, entityInformation.getJavaType());
    }

    @Override
    public <S extends T> UpdateResult updateBatch(List<S> entities) {
        if (entities == null || entities.size() == 0) {
            return UpdateResult.acknowledged(0L, 0L, null);
        }
        return entities.stream().map(this::update).reduce((updateResult, updateResult2) -> UpdateResult.acknowledged(
                updateResult.getMatchedCount() + updateResult2.getMatchedCount(),
                updateResult.getModifiedCount() + updateResult2.getModifiedCount(),
                null
        )).orElse(UpdateResult.unacknowledged());
    }

    @Override
    public <S extends T> UpdateResult update(Query query, Update update) {
        return mongoOperations.updateMulti(query, update, this.entityInformation.getCollectionName());
    }

    @Override
    public <S extends T> UpdateResult update(S entity, Query query) {
        return null;
    }

    @Override
    public DeleteResult delete(Query query) {
        return mongoOperations.remove(query, this.entityInformation.getJavaType());
    }
}
