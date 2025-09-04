package network;

// Shim to maintain backward compatibility for existing imports
public class Message extends network.io.Message {
    public Message(int command) { super(command); }
    public Message(byte command) { super(command); }
    public Message(byte command, byte[] data) { super(command, data); }
}
