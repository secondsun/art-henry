package org.aerogear.apps.memeolist.view;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import org.aerogear.apps.memeolist.MemeolistApplication;
import org.aerogear.apps.memeolist.MainActivity;
import org.aerogear.apps.memeolist.R;
import org.aerogear.apps.memeolist.events.ProjectsAvailable;
import org.aerogear.apps.memeolist.fh.FHClient;
import org.aerogear.apps.memeolist.util.RecyclerItemClickListener;
import org.aerogear.apps.memeolist.util.adapter.ProjectViewAdapter;
import org.aerogear.apps.memeolist.vo.Project;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by summers on 11/6/15.
 */
public class MainFragment extends Fragment {

    private static final String IS_TABLET = "MainFragment.IS_TABLET";
    @Bind(R.id.empty)
    View emptyView;

    @Bind(R.id.loading)
    View loadingView;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.art_cards_list)
    RecyclerView artCardsList;
    private ProjectViewAdapter adapter;

    @Inject
    FHClient fhClient;

    @Inject
    Bus bus;

    @Inject
    Picasso picasso;

    private boolean isTablet = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, null);
        ButterKnife.bind(this, view);

        ((MemeolistApplication) getActivity().getApplicationContext()).getObjectGraph().inject(this);
        setupToolbarMenu();
        //((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Meme Henry");
        adapter = new ProjectViewAdapter(getActivity().getApplicationContext());
        adapter.setPicasso(picasso);
        this.isTablet = getArguments().getBoolean(IS_TABLET, false);
        fhClient.refreshMemes();
        setupView();

        return view;
    }


    public static Fragment newInstance(boolean isTablet) {
        Bundle args = new Bundle();
        args.putBoolean(IS_TABLET, isTablet);
        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void setupView() {
        artCardsList.setLayoutManager(new GridLayoutManager(getActivity(), isTablet ? 3 : 1));
        artCardsList.setAdapter(adapter);
        artCardsList.addOnItemTouchListener(new RecyclerItemClickListener(
                getActivity().getApplicationContext(),
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        ((MainActivity) getActivity()).showPopup(adapter.getProject(position));
                    }
                }
        ));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }
        });
        itemTouchHelper.attachToRecyclerView(artCardsList);

    }


    @Override
    public void onResume() {
        super.onStart();
        bus.register(this);
    }

    @Override
    public void onPause() {
        super.onStop();
        bus.unregister(this);
    }

    @Subscribe
    public void memesAvailable(ProjectsAvailable event) {
        Set<Project> allData = new HashSet<>(event.getProjects());
        if (allData.size() > 0) {
            loadingView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            artCardsList.setVisibility(View.VISIBLE);
        } else {
            loadingView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            artCardsList.setVisibility(View.GONE);
            return;
        }

        adapter.removeMissingItemsFrom(allData);
        adapter.addNewItemsFrom(allData);

        adapter.notifyDataSetChanged();
    }


    private void setupToolbarMenu() {
        toolbar.inflateMenu(R.menu.main_menu);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                fhClient.getSyncClient().forceSync("photos");
                return true;
            }
        });
    }


}
