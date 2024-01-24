package jp.osdn.gokigen.aira01b.myolycameraprops;

public interface ICameraPropertyLoadSaveOperations
{
    void saveProperties(final String idHeader, final String dataName);
    void loadProperties(final String idHeader, final String dataName);
}
