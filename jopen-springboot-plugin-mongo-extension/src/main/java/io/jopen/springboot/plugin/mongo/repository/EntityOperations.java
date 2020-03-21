/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jopen.springboot.plugin.mongo.repository;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mongodb.core.MappedDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoWriter;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.MongoSimpleTypes;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Common operations performed on an entity in the context of it's mapping metadata.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.1
 * @see MongoTemplate
 * @see ReactiveMongoTemplate
 */
@RequiredArgsConstructor
public class EntityOperations {

    private static final String ID_FIELD = "_id";

    private final @NonNull MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> context;

    /**
     * Creates a new {@link Entity} for the given bean.
     *
     * @param entity must not be {@literal null}.
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Entity<T> forEntity(T entity) {

        Assert.notNull(entity, "Bean must not be null!");

        if (entity instanceof String) {
            return new UnmappedEntity(parse(entity.toString()));
        }

        if (entity instanceof Map) {
            return new SimpleMappedEntity((Map<String, Object>) entity);
        }

        return MappedEntity.of(entity, context);
    }

    /**
     * Creates a new {@link AdaptibleEntity} for the given bean and {@link ConversionService}.
     *
     * @param entity must not be {@literal null}.
     * @param conversionService must not be {@literal null}.
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> AdaptibleEntity<T> forEntity(T entity, ConversionService conversionService) {

        Assert.notNull(entity, "Bean must not be null!");
        Assert.notNull(conversionService, "ConversionService must not be null!");

        if (entity instanceof String) {
            return new UnmappedEntity(parse(entity.toString()));
        }

        if (entity instanceof Map) {
            return new SimpleMappedEntity((Map<String, Object>) entity);
        }

        return AdaptibleMappedEntity.of(entity, context, conversionService);
    }

    public String determineCollectionName(@Nullable Class<?> entityClass) {

        if (entityClass == null) {
            throw new InvalidDataAccessApiUsageException(
                    "No class parameter provided, entity collection can't be determined!");
        }

        return context.getRequiredPersistentEntity(entityClass).getCollection();
    }

    public Query getByIdInQuery(Collection<?> entities) {

        MultiValueMap<String, Object> byIds = new LinkedMultiValueMap<>();

        entities.stream() //
                .map(this::forEntity) //
                .forEach(it -> byIds.add(it.getIdFieldName(), it.getId()));

        Criteria[] criterias = byIds.entrySet().stream() //
                .map(it -> Criteria.where(it.getKey()).in(it.getValue())) //
                .toArray(Criteria[]::new);

        return new Query(criterias.length == 1 ? criterias[0] : new Criteria().orOperator(criterias));
    }

    /**
     * Returns the name of the identifier property. Considers mapping information but falls back to the MongoDB default of
     * {@code _id} if no identifier property can be found.
     *
     * @param type must not be {@literal null}.
     * @return
     */
    public String getIdPropertyName(Class<?> type) {

        Assert.notNull(type, "Type must not be null!");

        MongoPersistentEntity<?> persistentEntity = context.getPersistentEntity(type);

        if (persistentEntity != null && persistentEntity.getIdProperty() != null) {
            return persistentEntity.getRequiredIdProperty().getName();
        }

        return ID_FIELD;
    }

    /**
     * Return the name used for {@code $geoNear.distanceField} avoiding clashes with potentially existing properties.
     *
     * @param domainType must not be {@literal null}.
     * @return the name of the distanceField to use. {@literal dis} by default.
     * @since 2.2
     */
    public String nearQueryDistanceFieldName(Class<?> domainType) {

        MongoPersistentEntity<?> persistentEntity = context.getPersistentEntity(domainType);
        if (persistentEntity == null || persistentEntity.getPersistentProperty("dis") == null) {
            return "dis";
        }

        String distanceFieldName = "calculated-distance";
        int counter = 0;
        while (persistentEntity.getPersistentProperty(distanceFieldName) != null) {
            distanceFieldName += "-" + (counter++);
        }

        return distanceFieldName;
    }

    private static Document parse(String source) {

        try {
            return Document.parse(source);
        } catch (org.bson.json.JsonParseException o_O) {
            throw new MappingException("Could not parse given String to save into a JSON document!", o_O);
        } catch (RuntimeException o_O) {

            // legacy 3.x exception
            if (ClassUtils.matchesTypeName(o_O.getClass(), "JSONParseException")) {
                throw new MappingException("Could not parse given String to save into a JSON document!", o_O);
            }
            throw o_O;
        }
    }

    public <T> TypedOperations<T> forType(@Nullable Class<T> entityClass) {

        if (entityClass != null) {

            MongoPersistentEntity<?> entity = context.getPersistentEntity(entityClass);

            if (entity != null) {
                return new TypedEntityOperations(entity);
            }

        }
        return UntypedOperations.instance();
    }

    /**
     * A representation of information about an entity.
     *
     * @author Oliver Gierke
     * @since 2.1
     */
    interface Entity<T> {

        /**
         * Returns the field name of the identifier of the entity.
         *
         * @return
         */
        String getIdFieldName();

        /**
         * Returns the identifier of the entity.
         *
         * @return
         */
        Object getId();

        /**
         * Returns the {@link Query} to find the entity by its identifier.
         *
         * @return
         */
        Query getByIdQuery();

        /**
         * Returns the {@link Query} to remove an entity by its {@literal id} and if applicable {@literal version}.
         *
         * @return the {@link Query} to use for removing the entity. Never {@literal null}.
         * @since 2.2
         */
        default Query getRemoveByQuery() {
            return isVersionedEntity() ? getQueryForVersion() : getByIdQuery();
        }

        /**
         * Returns the {@link Query} to find the entity in its current version.
         *
         * @return
         */
        Query getQueryForVersion();

        /**
         * Maps the backing entity into a {@link MappedDocument} using the given {@link MongoWriter}.
         *
         * @param writer must not be {@literal null}.
         * @return
         */
        MappedDocument toMappedDocument(MongoWriter<? super T> writer);

        /**
         * Asserts that the identifier type is updatable in case its not already set.
         */
        default void assertUpdateableIdIfNotSet() {}

        /**
         * Returns whether the entity is versioned, i.e. if it contains a version property.
         *
         * @return
         */
        default boolean isVersionedEntity() {
            return false;
        }

        /**
         * Returns the value of the version if the entity {@link #isVersionedEntity() has a version property}.
         *
         * @return the entity version. Can be {@literal null}.
         * @throws IllegalStateException if the entity does not define a {@literal version} property. Make sure to check
         *           {@link #isVersionedEntity()}.
         */
        @Nullable
        Object getVersion();

        /**
         * Returns the underlying bean.
         *
         * @return
         */
        T getBean();

        /**
         * Returns whether the entity is considered to be new.
         *
         * @return
         * @since 2.1.2
         */
        boolean isNew();
    }

    /**
     * Information and commands on an entity.
     *
     * @author Oliver Gierke
     * @since 2.1
     */
    interface AdaptibleEntity<T> extends Entity<T> {

        /**
         * Populates the identifier of the backing entity if it has an identifier property and there's no identifier
         * currently present.
         *
         * @param id must not be {@literal null}.
         * @return
         */
        @Nullable
        T populateIdIfNecessary(@Nullable Object id);

        /**
         * Initializes the version property of the of the current entity if available.
         *
         * @return the entity with the version property updated if available.
         */
        T initializeVersionProperty();

        /**
         * Increments the value of the version property if available.
         *
         * @return the entity with the version property incremented if available.
         */
        T incrementVersion();

        /**
         * Returns the current version value if the entity has a version property.
         *
         * @return the current version or {@literal null} in case it's uninitialized.
         * @throws IllegalStateException if the entity does not define a {@literal version} property.
         */
        @Nullable
        Number getVersion();
    }

    @RequiredArgsConstructor
    private static class UnmappedEntity<T extends Map<String, Object>> implements AdaptibleEntity<T> {

        private final T map;

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getIdPropertyName()
         */
        @Override
        public String getIdFieldName() {
            return ID_FIELD;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getId()
         */
        @Override
        public Object getId() {
            return map.get(ID_FIELD);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getByIdQuery()
         */
        @Override
        public Query getByIdQuery() {
            return Query.query(Criteria.where(ID_FIELD).is(map.get(ID_FIELD)));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.MutablePersistableSource#populateIdIfNecessary(java.lang.Object)
         */
        @Nullable
        @Override
        public T populateIdIfNecessary(@Nullable Object id) {

            map.put(ID_FIELD, id);

            return map;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getQueryForVersion()
         */
        @Override
        public Query getQueryForVersion() {
            throw new MappingException("Cannot query for version on plain Documents!");
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#toMappedDocument(org.springframework.data.mongodb.core.convert.MongoWriter)
         */
        @Override
        public MappedDocument toMappedDocument(MongoWriter<? super T> writer) {
            return MappedDocument.of(map instanceof Document //
                    ? (Document) map //
                    : new Document(map));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.MutablePersistableSource#initializeVersionProperty()
         */
        @Override
        public T initializeVersionProperty() {
            return map;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.MutablePersistableSource#getVersion()
         */
        @Override
        @Nullable
        public Number getVersion() {
            return null;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.MutablePersistableSource#incrementVersion()
         */
        @Override
        public T incrementVersion() {
            return map;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getBean()
         */
        @Override
        public T getBean() {
            return map;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.Entity#isNew()
         */
        @Override
        public boolean isNew() {
            return map.get(ID_FIELD) != null;
        }
    }

    private static class SimpleMappedEntity<T extends Map<String, Object>> extends UnmappedEntity<T> {

        SimpleMappedEntity(T map) {
            super(map);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#toMappedDocument(org.springframework.data.mongodb.core.convert.MongoWriter)
         */
        @Override
        @SuppressWarnings("unchecked")
        public MappedDocument toMappedDocument(MongoWriter<? super T> writer) {

            T bean = getBean();
            bean = (T) (bean instanceof Document //
                    ? (Document) bean //
                    : new Document(bean));
            Document document = new Document();
            writer.write(bean, document);

            return MappedDocument.of(document);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static class MappedEntity<T> implements Entity<T> {

        private final @NonNull MongoPersistentEntity<?> entity;
        private final @NonNull IdentifierAccessor idAccessor;
        private final @NonNull PersistentPropertyAccessor<T> propertyAccessor;

        private static <T> MappedEntity<T> of(T bean,
                                              MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> context) {

            MongoPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
            IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
            PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

            return new MappedEntity<>(entity, identifierAccessor, propertyAccessor);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getIdPropertyName()
         */
        @Override
        public String getIdFieldName() {
            return entity.getRequiredIdProperty().getFieldName();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getId()
         */
        @Override
        public Object getId() {
            return idAccessor.getRequiredIdentifier();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getByIdQuery()
         */
        @Override
        public Query getByIdQuery() {

            if (!entity.hasIdProperty()) {
                throw new MappingException("No id property found for object of type " + entity.getType() + "!");
            }

            MongoPersistentProperty idProperty = entity.getRequiredIdProperty();

            return Query.query(Criteria.where(idProperty.getName()).is(getId()));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getQueryForVersion(java.lang.Object)
         */
        @Override
        public Query getQueryForVersion() {

            MongoPersistentProperty idProperty = entity.getRequiredIdProperty();
            MongoPersistentProperty versionProperty = entity.getRequiredVersionProperty();

            return new Query(Criteria.where(idProperty.getName()).is(getId())//
                    .and(versionProperty.getName()).is(getVersion()));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#toMappedDocument(org.springframework.data.mongodb.core.convert.MongoWriter)
         */
        @Override
        public MappedDocument toMappedDocument(MongoWriter<? super T> writer) {

            T bean = propertyAccessor.getBean();

            Document document = new Document();
            writer.write(bean, document);

            if (document.containsKey(ID_FIELD) && document.get(ID_FIELD) == null) {
                document.remove(ID_FIELD);
            }

            return MappedDocument.of(document);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.Entity#assertUpdateableIdIfNotSet()
         */
        public void assertUpdateableIdIfNotSet() {

            if (!entity.hasIdProperty()) {
                return;
            }

            MongoPersistentProperty property = entity.getRequiredIdProperty();
            Object propertyValue = idAccessor.getIdentifier();

            if (propertyValue != null) {
                return;
            }

            if (!MongoSimpleTypes.AUTOGENERATED_ID_TYPES.contains(property.getType())) {
                throw new InvalidDataAccessApiUsageException(
                        String.format("Cannot autogenerate id of type %s for entity of type %s!", property.getType().getName(),
                                entity.getType().getName()));
            }
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#isVersionedEntity()
         */
        @Override
        public boolean isVersionedEntity() {
            return entity.hasVersionProperty();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getVersion()
         */
        @Override
        @Nullable
        public Object getVersion() {
            return propertyAccessor.getProperty(entity.getRequiredVersionProperty());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.PersistableSource#getBean()
         */
        @Override
        public T getBean() {
            return propertyAccessor.getBean();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.Entity#isNew()
         */
        @Override
        public boolean isNew() {
            return entity.isNew(propertyAccessor.getBean());
        }
    }

    private static class AdaptibleMappedEntity<T> extends MappedEntity<T> implements AdaptibleEntity<T> {

        private final MongoPersistentEntity<?> entity;
        private final ConvertingPropertyAccessor<T> propertyAccessor;
        private final IdentifierAccessor identifierAccessor;

        private AdaptibleMappedEntity(MongoPersistentEntity<?> entity, IdentifierAccessor identifierAccessor,
                                      ConvertingPropertyAccessor<T> propertyAccessor) {

            super(entity, identifierAccessor, propertyAccessor);

            this.entity = entity;
            this.propertyAccessor = propertyAccessor;
            this.identifierAccessor = identifierAccessor;
        }

        private static <T> AdaptibleEntity<T> of(T bean,
                                                 MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> context,
                                                 ConversionService conversionService) {

            MongoPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
            IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
            PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

            return new AdaptibleMappedEntity<>(entity, identifierAccessor,
                    new ConvertingPropertyAccessor<>(propertyAccessor, conversionService));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.AdaptibleEntity#populateIdIfNecessary(java.lang.Object)
         */
        @Nullable
        @Override
        public T populateIdIfNecessary(@Nullable Object id) {

            if (id == null) {
                return propertyAccessor.getBean();
            }

            MongoPersistentProperty idProperty = entity.getIdProperty();
            if (idProperty == null) {
                return propertyAccessor.getBean();
            }

            if (identifierAccessor.getIdentifier() != null) {
                return propertyAccessor.getBean();
            }

            propertyAccessor.setProperty(idProperty, id);
            return propertyAccessor.getBean();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.MappedEntity#getVersion()
         */
        @Override
        @Nullable
        public Number getVersion() {

            MongoPersistentProperty versionProperty = entity.getRequiredVersionProperty();

            return propertyAccessor.getProperty(versionProperty, Number.class);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.AdaptibleEntity#initializeVersionProperty()
         */
        @Override
        public T initializeVersionProperty() {

            if (!entity.hasVersionProperty()) {
                return propertyAccessor.getBean();
            }

            MongoPersistentProperty versionProperty = entity.getRequiredVersionProperty();

            propertyAccessor.setProperty(versionProperty, versionProperty.getType().isPrimitive() ? 1 : 0);

            return propertyAccessor.getBean();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.AdaptibleEntity#incrementVersion()
         */
        @Override
        public T incrementVersion() {

            MongoPersistentProperty versionProperty = entity.getRequiredVersionProperty();
            Number version = getVersion();
            Number nextVersion = version == null ? 0 : version.longValue() + 1;

            propertyAccessor.setProperty(versionProperty, nextVersion);

            return propertyAccessor.getBean();
        }
    }

    /**
     * Type-specific operations abstraction.
     *
     * @author Mark Paluch
     * @param <T>
     * @since 2.2
     */
    interface TypedOperations<T> {

        /**
         * Return the optional {@link Collation} for the underlying entity.
         *
         * @return
         */
        Optional<Collation> getCollation();

        /**
         * Return the optional {@link Collation} from the given {@link Query} and fall back to the collation configured for
         * the underlying entity.
         *
         * @return
         */
        Optional<Collation> getCollation(Query query);
    }

    /**
     * {@link TypedOperations} for generic entities that are not represented with {@link PersistentEntity} (e.g. custom
     * conversions).
     */
    @RequiredArgsConstructor
    enum UntypedOperations implements TypedOperations<Object> {

        INSTANCE;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T> TypedOperations<T> instance() {
            return (TypedOperations) INSTANCE;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.TypedOperations#getCollation()
         */
        @Override
        public Optional<Collation> getCollation() {
            return Optional.empty();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.TypedOperations#getCollation(org.springframework.data.mongodb.core.query.Query)
         */
        @Override
        public Optional<Collation> getCollation(Query query) {

            if (query == null) {
                return Optional.empty();
            }

            return query.getCollation();
        }
    }

    /**
     * {@link TypedOperations} backed by {@link MongoPersistentEntity}.
     *
     * @param <T>
     */
    @RequiredArgsConstructor
    static class TypedEntityOperations<T> implements TypedOperations<T> {

        private final @NonNull MongoPersistentEntity<T> entity;

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.TypedOperations#getCollation()
         */
        @Override
        public Optional<Collation> getCollation() {
            return Optional.ofNullable(entity.getCollation());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.core.EntityOperations.TypedOperations#getCollation(org.springframework.data.mongodb.core.query.Query)
         */
        @Override
        public Optional<Collation> getCollation(Query query) {

            if (query.getCollation().isPresent()) {
                return query.getCollation();
            }

            return Optional.ofNullable(entity.getCollation());
        }
    }

}
