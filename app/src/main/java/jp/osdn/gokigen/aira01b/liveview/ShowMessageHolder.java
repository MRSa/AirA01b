package jp.osdn.gokigen.aira01b.liveview;

import android.content.Context;
import android.graphics.Color;

class ShowMessageHolder implements IMessageDrawer
{

    private class messageHolder
    {
        private String message = "";
        private int color = Color.BLUE;
        private int textSize = 16;

        String getMessage()
        {
            return message;
        }

        void setMessage(String message)
        {
            this.message = message;
        }

        int getColor()
        {
            return color;
        }

        void setColor(int color)
        {
            this.color = color;
        }

        int getTextSize()
        {
            float density = context.getResources().getDisplayMetrics().density;
            int fontSize = (int)(textSize * density);
            if (fontSize <= 0)
            {
                fontSize = textSize;
            }
            return (fontSize);
        }

        void setTextSize(int textSize)
        {
            this.textSize = textSize;
        }
    }

    private final messageHolder upperLeft = new messageHolder();
    private final messageHolder upperRight = new messageHolder();
    private final messageHolder center = new messageHolder();
    private final messageHolder lowerLeft = new messageHolder();
    private final messageHolder lowerRight = new messageHolder();
    private final messageHolder upperCenter = new messageHolder();
    private final messageHolder lowerCenter = new messageHolder();

    private float level_horizontal = Float.NaN;
    private float level_vertical = Float.NaN;

    private final Context context;

    ShowMessageHolder(Context context)
    {
        this.context = context;
        center.setTextSize(24);
    }

    /**
     *
     *
     */
    private messageHolder decideHolder(MessageArea area)
    {
        return switch (area) {
            case CENTER -> center;
            case UPLEFT -> upperLeft;
            case UPRIGHT -> upperRight;
            case LOWLEFT -> lowerLeft;
            case LOWRIGHT -> lowerRight;
            case UPCENTER -> upperCenter;
            case LOWCENTER -> lowerCenter;
        };
    }

    /**
     *
     *
     */
    @Override
    public void setMessageToShow(MessageArea area, int color, int size, String message)
    {
        messageHolder target = decideHolder(area);
        if (target != null)
        {
            target.setColor(color);
            target.setTextSize(size);
            target.setMessage(message);
        }
    }

    /**
     *
     *
     */
    @Override
    public void setLevelToShow(LevelArea area, float value)
    {
        if (area == LevelArea.LEVEL_HORIZONTAL)
        {
            level_horizontal = value;
        }
        else if (area == LevelArea.LEVEL_VERTICAL)
        {
           level_vertical = value;
        }
    }

    /**
     *
     *
     */
    int getSize(MessageArea area)
    {
        messageHolder target = decideHolder(area);
        if (target != null)
        {
            return (target.getTextSize());
        }
        return (0);
    }

    /**
     *
     *
     */
    int getColor(MessageArea area)
    {
        messageHolder target = decideHolder(area);
        if (target != null)
        {
            return (target.getColor());
        }
        return (0);
    }

    /**
     *
     *
     */
    String getMessage(MessageArea area)
    {
        messageHolder target = decideHolder(area);
        if (target != null)
        {
            return (target.getMessage());
        }
        return ("");
    }

    boolean isLevel()
    {
        return (!((Float.isNaN(level_horizontal))||(Float.isNaN(level_vertical))));
    }

    /**
     *
     *
     */
    float getLevel(LevelArea area)
    {
        float value;
        if (area == LevelArea.LEVEL_HORIZONTAL)
        {
            value = level_horizontal;
        }
        else //  if (area == LevelArea.LEVEL_VERTICAL)
        {
            value = level_vertical;
        }
        return (value);
    }

    /**
     *   傾きの色を取得する
     *
     */
    int getLevelColor(float value)
    {
        value = Math.abs(value);

        float LEVELGAUGE_THRESHOLD_MIDDLE = 2.0f;
        if (value < LEVELGAUGE_THRESHOLD_MIDDLE)
        {
            return (Color.GREEN);
        }
        float LEVELGAUGE_THRESHOLD_OVER = 15.0f;
        if (value > LEVELGAUGE_THRESHOLD_OVER)
        {
            return (Color.RED);
        }
        return (Color.YELLOW);
    }
}
