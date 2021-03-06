package com.shawn.researjour.Adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shawn.researjour.Activity.AddNewPost;
import com.shawn.researjour.Activity.PostDetailActivity;
import com.shawn.researjour.Activity.ThereProfileActivity;
import com.shawn.researjour.Models.ModelClassPost;
import com.shawn.researjour.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.MyHolder> {

    Context context;
    List<ModelClassPost> postList;

    String myUid;

    private DatabaseReference likesRef,postsRef;

    boolean mProcessLike=false;
    private int countLikes;

    public PostsAdapter(Context context, List<ModelClassPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid=FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef=FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef=FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        //inflate layout research post layout
        View view= LayoutInflater.from(context)
                .inflate(R.layout.research_post_layout,viewGroup,false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder myHolder, final int i) {

        //get the information of the user from fireBase
        final FirebaseUser firebaseUser= FirebaseAuth.getInstance().getCurrentUser();
        final ModelClassPost post=postList.get(i);
        //get data
        final String uid=postList.get(i).getUid();
        String uEmail=postList.get(i).getuEmail();
        final String postid=postList.get(i).getPostid();
        String pLikes=postList.get(i).getpLikes();
        String pComments=postList.get(i).getpComments();
        String uName=postList.get(i).getuName();
        String uDp=postList.get(i).getuDp();
        final String pTitle=postList.get(i).getTitle();
        final String pAbstraction=postList.get(i).getAbstraction();
        final String pVideo=postList.get(i).getVideoLink();
        final String pImage=postList.get(i).getPostimage();
        String pTime=postList.get(i).getpTime();

        //convert timestamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar=Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTime));
        String pTimeFormat= DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        //set data
        myHolder.uNameTv.setText(uName);
        myHolder.pTimeTv.setText(pTimeFormat);
        myHolder.pTitleTv.setText(pTitle);
        myHolder.pAbstractionTv.setText(pAbstraction);
        myHolder.pVideoLink.setText(pVideo);
        myHolder.pLikesTv.setText(pLikes+" upvotes");//e.g. 100 upvotes
        myHolder.pCommentTv.setText(pComments+" Feedbacks");//e.g. 100 upvotes

        //set likes for each post
        setLikes(myHolder,postid);

        Picasso.get().load(uDp).placeholder(R.drawable.user_profile).into(myHolder.uPictureIv);


        //set post picture
        //if there is no picture in the post
        if (pImage.equals("noImage")){
            //hide imageView
            myHolder.pImageIv.setVisibility(View.GONE);

        }else {
            myHolder.pImageIv.setVisibility(View.VISIBLE);
            try{

                Picasso.get().load(pImage).into(myHolder.pImageIv);

            }catch (Exception e){

            }
        }

        //isSaved(post.getPostid(),myHolder.bookmarkBtn);

        myHolder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*click to go to specific user profile activity with the uid, this uid is of the clicked user
                * which will be used to show user specific data/posts*/
                Intent intent= new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid",uid);
                context.startActivity(intent);
            }
        });

        //handle button clicks
        myHolder.moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions(myHolder.moreButton,uid,myUid,postid,pImage);
            }
        });

        myHolder.admireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*get total number of likes for the post, whose like button clicked
                 * if currently signed in user has not liked it before
                 * increase value by 1, otherwise decrease value by 1*/
                mProcessLike=true;
                final int pLikes=Integer.parseInt(postList.get(i).getpLikes());

                //get the id of the post clicked
                final String postIde=postList.get(i).getPostid();

                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (mProcessLike){
                            if (dataSnapshot.child(postIde).hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                //already liked, so remove like
                                likesRef.child(postIde).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).removeValue();
                                /*postsRef.child(postIde).child("pLikes").setValue(""+(pLikes-1));
                                notifyDataSetChanged();*/
                                mProcessLike=false;
                            }else {
                                //not liked, like it
                                likesRef.child(postIde).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue("upvoted");
                               /* postsRef.child(postIde).child("pLikes").setValue(""+(pLikes+1));
                                notifyDataSetChanged();*/
                                mProcessLike=false;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        myHolder.feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    //start PostDetailActivity
                    Intent intent=new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId",postid);
                    context.startActivity(intent);
            }
        });

        myHolder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get image from imageview
                BitmapDrawable bitmapDrawable=(BitmapDrawable)myHolder.pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    //post without image
                    shareTextOnly(pTitle,pAbstraction);
                }else {
                    //post with image
                    Bitmap bitmap=bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle,pAbstraction,bitmap);

                }

            }
        });
    }

    private void shareTextOnly(String pTitle, String pAbstraction) {
        String shareBody=pTitle+"\n"+pAbstraction;

        Intent sIntent=new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        context.startActivity(Intent.createChooser(sIntent,"Share Via"));
    }

    private void shareImageAndText(String pTitle, String pAbstraction, Bitmap bitmap) {
        String shareBody=pTitle+"\n"+pAbstraction;

        Uri uri=saveImageToShare(bitmap);

        //share intent
        Intent sIntent=new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM,uri);
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        sIntent.setType("image/png");
        context.startActivity(Intent.createChooser(sIntent,"Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder=new File(context.getCacheDir(),"images");
        Uri uri=null;
        try {
            imageFolder.mkdirs();
            File file=new File(imageFolder,"shared_image.png");

            FileOutputStream stream=new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri= FileProvider.getUriForFile(context,"com.shawn.researjour.fileprovider",file);
            
        }catch (Exception e){
            Toast.makeText(context, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void showMoreOptions(ImageButton moreButton, String uid, String myUid, final String postid, final String pImage) {

        //creating popup menu currently having option delete, we will add more options later
        PopupMenu popupMenu=new PopupMenu(context, moreButton, Gravity.END);

        //show delete option in only post of currently signed-in user
        if (uid.equals(myUid)){
            //add item in menu
            popupMenu.getMenu().add(Menu.NONE,1,0,"Edit Post");
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete Post");
        }
        popupMenu.getMenu().add(Menu.NONE,2,0,"View Research Post");
        //menu item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id=menuItem.getItemId();
                if (id==0){
                    //delete is clicked
                    beginDelete(postid,pImage);
                }else if (id==1){
                    //edit is clicked
                    /*start add new post activity with key "edit post" and the id of the post clicked*/
                    Intent intent=new Intent(context, AddNewPost.class);
                    intent.putExtra("key","editPost");
                    intent.putExtra("editPostId",postid);
                    context.startActivity(intent);
                }else if (id==2){
                    Intent intent=new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId",postid);
                    context.startActivity(intent);
                }

                return false;
            }
        });
        //show menu
        popupMenu.show();
    }

    private void beginDelete(String postid, String pImage) {
        if (pImage.equals("noImage")){
            //post without image
            deleteWithoutImage(postid);
        }else {
            //post with image
            deleteWithImage(postid,pImage);
        }

    }

    private void deleteWithImage(final String postid, String pImage) {
        //progress bar
        final ProgressDialog pd=new ProgressDialog(context);
        pd.setMessage("Deleting Post..");
        /*steps:
         * 1.Delete Image using url
         * 2.Delete from database using post id*/
        StorageReference picRef= FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //image deleted
                Query fquery=FirebaseDatabase.getInstance().getReference("Posts")
                        .orderByChild("postid").equalTo(postid);
                fquery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot dataSnapshot1:dataSnapshot.getChildren()){
                            dataSnapshot1.getRef().removeValue();//remove valued from firebase where pid matches
                        }
                        //deleted
                        Toast.makeText(context, "Research Post Deleted", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //failed
                pd.dismiss();
                Toast.makeText(context, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void deleteWithoutImage(String postid) {
        //progress bar
        final ProgressDialog pd=new ProgressDialog(context);
        pd.setMessage("Deleting Post..");

        Query fquery=FirebaseDatabase.getInstance().getReference("Posts")
                .orderByChild("postid").equalTo(postid);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds:dataSnapshot.getChildren()){
                    ds.getRef().removeValue();
                }
                //deleted
                Toast.makeText(context, "Research Post Deleted", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLikes(final MyHolder holder, final String postid) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postid).hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                    //user liked this post
                    /*to indicate that the post is liked by this signed in user
                 * change drawable left icon of like button
                 * change text of like button from "upvote" to "upvoted"*/
                    countLikes=(int)dataSnapshot.child(postid).getChildrenCount();
                    holder.admireButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.liked,0,0,0);
                    holder.pLikesTv.setText(Integer.toString(countLikes)+" upvotes");
                    holder.admireButton.setText("upvoted");

                }else {
                    //user liked this post
                    /*to indicate that the post is liked by this signed in user
                 * change drawable left icon of like button
                 * change text of like button from "upvoted" to "upvote"*/
                    countLikes=(int)dataSnapshot.child(postid).getChildrenCount();
                    holder.pLikesTv.setText(Integer.toString(countLikes)+" upvotes");
                    holder.admireButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.like,0,0,0);
                    holder.admireButton.setText("upvote");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{


        //views from research_post_layout.xml
        ImageView uPictureIv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv,pAbstractionTv,pVideoLink,pVideoLinkText,pLikesTv,pCommentTv;
        ImageButton moreButton;
        Button admireButton, feedbackButton, shareBtn;
        RelativeLayout profileLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            profileLayout=itemView.findViewById(R.id.researcherProfileLayout_id);
            uPictureIv=itemView.findViewById(R.id.homeProfileImage_id);
            pImageIv=itemView.findViewById(R.id.researchPostImage_id);
            uNameTv=itemView.findViewById(R.id.homeResearcherName_id);
            pTimeTv=itemView.findViewById(R.id.homePostTime_id);
            pTitleTv=itemView.findViewById(R.id.homePostTitleText_id);
            pAbstractionTv=itemView.findViewById(R.id.homePostDescText_id);
            pVideoLinkText=itemView.findViewById(R.id.postVideoLinkText_id);
            pVideoLink=itemView.findViewById(R.id.postVideoLink_id);
            pLikesTv=itemView.findViewById(R.id.likeCounterText_id);
            pCommentTv=itemView.findViewById(R.id.CommentCounterText_id);
            moreButton=itemView.findViewById(R.id.moreButton_id);
            admireButton=itemView.findViewById(R.id.admire_btn_id);
            feedbackButton=itemView.findViewById(R.id.feedback_btn_id);
            shareBtn=itemView.findViewById(R.id.shareBtn_id);
        }
    }

}