package jp.osdn.gokigen.aira01b.olycamerawrapper.ble;

public interface IOlyCameraEntryList
{
    int MAX_STORE_PROPERTIES = 16;  // Olympus Airは、最大10個登録可能
    String NAME_KEY = "AirBtName";
    String CODE_KEY = "AirBtCode";
    String DATE_KEY = "AirBtId";
}
