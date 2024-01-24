package jp.osdn.gokigen.aira01b.olycameraproperty;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CameraPropertyArrayAdapter extends ArrayAdapter<CameraPropertyArrayItem>
{
    private LayoutInflater inflater = null;
    private final int textViewResourceId;
    private List<CameraPropertyArrayItem> listItems = null;

    public CameraPropertyArrayAdapter(Context context, int textId, List<CameraPropertyArrayItem> items)
    {
        super(context, textId, items);

        textViewResourceId = textId;
        listItems = items;

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     *
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view;
        if(convertView != null)
        {
            view = convertView;
        }
        else
        {
            view = inflater.inflate(textViewResourceId, null);
        }

        CameraPropertyArrayItem item = listItems.get(position);

        ImageView imageView = (ImageView) view.findViewWithTag("icon");
        imageView.setImageResource(item.getIconResource());

        TextView titleView = (TextView)view.findViewWithTag("name");
        titleView.setText(item.getPropertyName());

        TextView detailView = (TextView)view.findViewWithTag("title");
        detailView.setText(item.getPropertyTitle());

        TextView optionView = (TextView)view.findViewWithTag("value");
        optionView.setText(item.getPropertyValueTitle());

        return (view);
    }

}
