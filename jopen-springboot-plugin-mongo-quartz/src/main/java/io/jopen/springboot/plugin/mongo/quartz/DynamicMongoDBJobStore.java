package io.jopen.springboot.plugin.mongo.quartz;

import com.mongodb.MongoClient;
import io.jopen.springboot.plugin.mongo.quartz.clojure.DynamicClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;

public class DynamicMongoDBJobStore extends MongoDBJobStore {

    public DynamicMongoDBJobStore() {
        super();
    }

    public DynamicMongoDBJobStore(MongoClient mongo) {
        super(mongo);
    }

    public DynamicMongoDBJobStore(String mongoUri, String username, String password) {
        super(mongoUri, username, password);
    }

    @Override
    protected ClassLoadHelper getClassLoaderHelper(ClassLoadHelper original) {
        return new DynamicClassLoadHelper();
    }
}
