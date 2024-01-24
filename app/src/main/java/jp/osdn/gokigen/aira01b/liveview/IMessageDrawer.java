package jp.osdn.gokigen.aira01b.liveview;

public interface IMessageDrawer
{
    // メッセージを表示する位置
    enum MessageArea
    {
        UPLEFT,
        UPRIGHT,
        CENTER,
        LOWLEFT,
        LOWRIGHT,
        UPCENTER,
        LOWCENTER,
    }

    enum LevelArea
    {
        LEVEL_HORIZONTAL,
        LEVEL_VERTICAL,
    }

    int SIZE_STD = 16;
    int SIZE_LARGE = 24;
    int SIZE_BIG = 32;

    void setMessageToShow(MessageArea area, int color, int size, String message);
    void setLevelToShow(LevelArea area, float value);

}
