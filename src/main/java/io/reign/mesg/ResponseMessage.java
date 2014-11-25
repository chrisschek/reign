package io.reign.mesg;

/**
 * 
 * @author ypai
 * 
 */
public interface ResponseMessage<T> extends Message<T> {
    public ResponseStatus getStatus();

    public String getComment();

    public void setComment(String comment);

    public ResponseMessage<T> setStatus(ResponseStatus status);

    public ResponseMessage<T> setStatus(ResponseStatus status, String comment);
}
