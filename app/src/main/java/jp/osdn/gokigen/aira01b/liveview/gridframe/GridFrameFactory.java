package jp.osdn.gokigen.aira01b.liveview.gridframe;

/**
 *
 *
 */
public class GridFrameFactory
{
    private static final int GRID_FRAME_0 = 0;
    private static final int GRID_FRAME_1 = 1;
    private static final int GRID_FRAME_2 = 2;
    private static final int GRID_FRAME_3 = 3;
    private static final int GRID_FRAME_4 = 4;
    private static final int GRID_FRAME_5 = 5;
    private static final int GRID_FRAME_6 = 6;

    public static IGridFrameDrawer getGridFrameDrawer(int id)
    {
        IGridFrameDrawer drawer;
        switch (id)
        {
            case GRID_FRAME_2:
                drawer = new GridFrameDrawer2();
                break;
            case GRID_FRAME_3:
                drawer = new GridFrameDrawer3();
                break;
            case GRID_FRAME_4:
                drawer = new GridFrameDrawer4();
                break;
            case GRID_FRAME_5:
                drawer = new GridFrameDrawer5();
                break;
            case GRID_FRAME_6:
                drawer = new GridFrameDrawer6();
                break;
            case GRID_FRAME_1:
                drawer = new GridFrameDrawer1();
                break;
            case GRID_FRAME_0:
            default:
                drawer = new GridFrameDrawer0();
                break;
        }
        return (drawer);
    }
}
