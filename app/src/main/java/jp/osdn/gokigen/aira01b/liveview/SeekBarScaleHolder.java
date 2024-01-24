package jp.osdn.gokigen.aira01b.liveview;

import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

public class SeekBarScaleHolder implements SeekBar.OnSeekBarChangeListener, ISeekbarPosition
{
    private final View parent;
    int currentValue;
    int maxValue;

    SeekBarScaleHolder(@NonNull View parent, int maxValue)
    {
        this.parent = parent;
        currentValue = 0;
        this.maxValue = maxValue;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        currentValue = progress;
        parent.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {

    }

    @Override
    public int getMaxPosition()
    {
        return (maxValue);
    }

    @Override
    public int getPosition()
    {
        return (currentValue);
    }

}
