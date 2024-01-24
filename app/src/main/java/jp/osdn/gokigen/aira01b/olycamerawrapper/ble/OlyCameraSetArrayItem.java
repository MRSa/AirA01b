package jp.osdn.gokigen.aira01b.olycamerawrapper.ble;

class OlyCameraSetArrayItem
{
    private final String dataId;
    private String btName = "";
    private String btPassCode = "";
    private String information = "";
/*
    private final String initialBtName;
    private final String initialBtPassCode;
    private final String initialInformation;
*/

    OlyCameraSetArrayItem(String dataId, String name, String passCode, String information)
    {
        this.dataId = dataId;
        this.btName = name;
        this.btPassCode = passCode;
        this.information = information;
/*
        this.initialBtName = name;
        this.initialBtPassCode = passCode;
        this.initialInformation = information;
*/
    }

    String getDataId()
    {
        return (dataId);
    }

    String getBtName()
    {
        return (btName);
    }

    String getBtPassCode()
    {
        return (btPassCode);
    }

    String getInformation()
    {
        return (information);
    }

/*
    void setValue(String btName, String btPassCode, String information)
    {
        this.btName = btName;
        this.btPassCode = btPassCode;
        this.information = information;
    }

    public boolean isChanged()
    {
        return (!(btName.equals(initialBtName))&&(btPassCode.equals(initialBtPassCode))&&(information.equals(initialInformation)));
    }

    public void resetValue()
    {
        this.btName = initialBtName;
        this.btPassCode = initialBtPassCode;
        this.information = initialInformation;
    }
*/
}
