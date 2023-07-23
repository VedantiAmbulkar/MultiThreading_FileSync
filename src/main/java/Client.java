public interface Client {

    public default void setTrigger(String trigger) {};

    public default void setTrigger(String trigger, String fileName) {};
}
