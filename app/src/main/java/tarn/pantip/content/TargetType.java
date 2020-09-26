package tarn.pantip.content;

public enum TargetType
{
    Topic(1),
    Comment(2),
    Reply(3);

    private final int type;

    TargetType(int type)
    {
        this.type = type;
    }

    int getValue()
    {
        return type;
    }
}
