package jp.osdn.gokigen.aira01b.olycamerawrapper;

/**
 *   使用するカメラプロパティのキー一覧
 *
 *
 */
public interface IOlyCameraProperty
{
    String TAKE_MODE = "TAKEMODE";
    String COLOR_TONE = "COLORTONE";
    String AE_MODE = "AE";
    String WB_MODE = "WB";
    String EXPOSURE_COMPENSATION = "EXPREV";
    String SHUTTER_SPEED = "SHUTTER";
    String APERTURE ="APERTURE";
    String ISO_SENSITIVITY = "ISO";
    String SOUND_VOLUME_LEVEL = "SOUND_VOLUME_LEVEL";
    String RAW = "RAW";
    String DRIVE_MODE = "TAKE_DRIVE";
    String BATTERY_LEVEL = "BATTERY_LEVEL";
    String AE_LOCK_STATE = "AE_LOCK_STATE";
    String ART_FILTER = "RECENTLY_ART_FILTER";

    String GPS = "GPS";
    String GPS_PROPERTY_ON = "<GPS/ON>";
    String GPS_PROPERTY_OFF = "<GPS/OFF>";

    String TAKE_MODE_MOVIE = "<TAKEMODE/movie>";

    String DRIVE_MODE_SINGLE = "<TAKE_DRIVE/DRIVE_NORMAL>";

    String FOCUS_STILL = "FOCUS_STILL";

    String FOCUS_MF = "FOCUS_MF";
    String FOCUS_SAF = "FOCUS_SAF";
}
