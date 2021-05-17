package com.hwaya.candytest;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EntriesAdapter  extends RecyclerView.Adapter<EntriesAdapter.ViewHolder> {
    List<EntryModel> list;


    public EntriesAdapter(List<EntryModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry,parent,false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.myText1.setText(list.get(position).getName());
        holder.myText2.setText(list.get(position).getCategory());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(holder.itemView.getContext(),EntryDetails.class);
                intent.putExtra("Name",list.get(position).getName());
                intent.putExtra("Username",list.get(position).getUsername());
                intent.putExtra("Password",list.get(position).getPassword());
                intent.putExtra("Website",list.get(position).getWebsite());
                intent.putExtra("Category",list.get(position).getCategory());
                intent.putExtra("Notes",list.get(position).getNotes());
                intent.putExtra("position",position);
                holder.itemView.getContext().startActivity(intent);

            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView myText1, myText2;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            myText1 = itemView.findViewById(R.id.Name);
            myText2 = itemView.findViewById(R.id.Refactor);
        }
    }
}
