package io.jopen.springboot.plugin.mongo.template.builder;

import org.apache.logging.log4j.util.Strings;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author maxuefeng
 * <p>
 * 字段映射
 * @see org.springframework.data.mongodb.core.mapping.Field
 * @see org.springframework.data.mongodb.core.mapping.FieldType
 * <p>
 * <p>
 * 实体回调
 * @see org.springframework.data.mapping.callback.EntityCallback
 * @see org.springframework.data.mongodb.core.mapping.event.BeforeSaveCallback
 * @see org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
 * @see MongoTemplate#execute(Class, CollectionCallback)
 * @see CollectionCallback
 * @see org.springframework.data.repository.PagingAndSortingRepository
 * @since 2019-11-13
 */
public class Builder<T> {

    Class<T> clazz;

    MongoTemplate mongoTemplate;

    /**
     * 缓存数据
     *
     * @see com.google.common.collect.MapMaker
     * @see com.google.common.cache.CacheBuilder
     */
    private static final ConcurrentHashMap<Class<?>, WeakReference<SerializedLambda>> SF_CACHE = new ConcurrentHashMap<>();

    // Cache<Object, Object> cache = CacheBuilder.newBuilder().weakKeys().build();

    @NonNull
    Function<SFunction<T, ?>, String> produceValName = sFunction -> {
        WeakReference<SerializedLambda> weakReference = SF_CACHE.get(sFunction.getClass());
        SerializedLambda serializedLambda = Optional.ofNullable(weakReference)
                .map(Reference::get)
                .orElseGet(() -> {
                    SerializedLambda lambda = SerializedLambda.resolve(sFunction);
                    SF_CACHE.put(sFunction.getClass(), new WeakReference<>(lambda));
                    return lambda;
                });
        return this.resolve(serializedLambda);
    };


    @Nullable
    private String resolve(@NonNull SerializedLambda lambda) {

        String implMethodName = lambda.getImplMethodName();

        // 忽略大小写
        String valName;
        if (implMethodName.startsWith("get")) {
            valName = implMethodName.replaceFirst("get", "");
        } else if (implMethodName.startsWith("is")) {
            valName = implMethodName.replaceFirst("is", "");
        } else {
            System.err.println("字段错误");
            return null;
        }

        String eqName = "";
        PropertyDescriptor[] descriptors = ReflectUtils.getBeanProperties(clazz);

        for (PropertyDescriptor descriptor : descriptors) {
            if (valName.equalsIgnoreCase(descriptor.getName())) {
                String finalValName = valName.substring(0, 1).toLowerCase() + valName.substring(1);
                Field field;
                try {
                    field = clazz.getDeclaredField(finalValName);
                } catch (Exception ignored) {
                    return null;
                }
                field.setAccessible(true);
                org.springframework.data.mongodb.core.mapping.Field fanno =
                        field.getAnnotation(org.springframework.data.mongodb.core.mapping.Field.class);


                if (fanno != null && Strings.isNotEmpty(fanno.value())) {
                    eqName = fanno.value();
                    break;
                } else {
                    eqName = finalValName;
                    break;
                }
            }
        }
        return eqName;
    }

}
