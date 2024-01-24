package jp.osdn.gokigen.aira01b;

import java.util.Map;

public class CachedImageData
{
    private final byte[] image;
    private final Map<String, Object> metadata;

    public CachedImageData(byte[] image, Map<String, Object> metadata)
    {
        this.image = image;
        this.metadata = metadata;
    }

    public byte[] getImage()
    {
        return (image);
    }

    public Map<String, Object> getMetadata()
    {
        return (metadata);
    }
}
