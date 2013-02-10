package sabikoi.app.coiltwitter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PostArrayAdapter extends ArrayAdapter<PostArrayItem>
{
  private int                 layoutID;
  private List<PostArrayItem> items;
  private LayoutInflater      inflater;
  
	public PostArrayAdapter(Context context,int layoutID, List<PostArrayItem> items)
	{
		super(context,layoutID,items);
		this.layoutID = layoutID;
		this.items = items;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	//��v�f���̃r���[�̐���
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view;
		if(convertView != null)
			view=convertView;
		else
			view=inflater.inflate(layoutID, null);
		
		//�A�C�e���̎擾
		PostArrayItem item=items.get(position);
		
		//���[�U�[��
		TextView userID = (TextView)view.findViewWithTag("userID");
		userID.setText(item.name);
		
		//post���e
		TextView content = (TextView)view.findViewWithTag("content");
		content.setText(item.content);
		
		//���t
		TextView date = (TextView)view.findViewWithTag("date");
		date.setText(item.date.toGMTString());
		
		return view;
	}
}
