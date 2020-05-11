package io.jopen.springboot.plugin.idempotency;

/**
 * @author maxuefeng
 * @since 2020/5/7
 */
public interface ApiIdempotentSupplier<T extends RuntimeException> {

    T apply();
}
