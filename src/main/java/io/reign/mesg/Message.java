package io.reign.mesg;

/**
 * 
 * @author ypai
 * 
 */
public interface Message<T> {

    public Integer getId();

    public Message<T> setId(Integer id);

    public T getBody();

    public Message<T> setBody(T body);

}
