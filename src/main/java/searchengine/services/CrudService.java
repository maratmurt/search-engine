package searchengine.services;

import java.util.Collection;

public interface CrudService<T> {
    T getById(Integer id);
    T create(T item);
    void update(T item);
    void delete(Integer id);
}
