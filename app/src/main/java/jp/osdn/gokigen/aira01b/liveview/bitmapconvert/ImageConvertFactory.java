package jp.osdn.gokigen.aira01b.liveview.bitmapconvert;

/**
 *
 *
 */
public class ImageConvertFactory
{
    private static final int CONVERT_TYPE_0 = 0;
    private static final int CONVERT_TYPE_1 = 1;
    private static final int CONVERT_TYPE_2 = 2;
    private static final int CONVERT_TYPE_3 = 3;
    private static final int CONVERT_TYPE_4 = 4;
    private static final int CONVERT_TYPE_5 = 5;
    private static final int CONVERT_TYPE_6 = 6;

    public static IPreviewImageConverter getImageConverter(int id)
    {
        IPreviewImageConverter drawer;
        switch (id)
        {
            case CONVERT_TYPE_6:
                drawer = new ConvertEdgeSobel();
                break;
            case CONVERT_TYPE_5:
                drawer = new ConvertEdgeLaplacian();
                break;
            case CONVERT_TYPE_4:
                drawer = new ConvertEdgeCanny();
                break;
            case CONVERT_TYPE_3:
                drawer = new ConvertBlackWhiteAndNegativePositive();
                break;
            case CONVERT_TYPE_2:
                drawer = new ConvertEdgeSobel();
                break;
            case CONVERT_TYPE_1:
                drawer = new ConvertEdgeColorY();
                break;
            case CONVERT_TYPE_0:
            default:
                drawer = new ConvertEdgeColorX();
                break;
        }
        return (drawer);
    }
}
