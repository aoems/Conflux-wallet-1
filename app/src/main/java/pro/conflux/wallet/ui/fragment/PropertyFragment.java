package pro.conflux.wallet.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import pro.conflux.wallet.C;
import pro.conflux.wallet.R;
import pro.conflux.wallet.base.BaseFragment;
import pro.conflux.wallet.domain.CfxWallet;
import pro.conflux.wallet.entity.Ticker;
import pro.conflux.wallet.entity.Token;
import pro.conflux.wallet.interact.FetchWalletInteract;
import pro.conflux.wallet.ui.activity.AddTokenActivity;
import pro.conflux.wallet.ui.activity.CreateWalletActivity;
import pro.conflux.wallet.ui.activity.GatheringQRCodeActivity;
import pro.conflux.wallet.ui.activity.PropertyDetailActivity;
import pro.conflux.wallet.ui.activity.QRCodeScannerActivity;
import pro.conflux.wallet.ui.activity.SendActivity;
import pro.conflux.wallet.ui.activity.TokenidListActivity;
import pro.conflux.wallet.ui.activity.WalletDetailActivity;
import pro.conflux.wallet.ui.activity.WalletMangerActivity;
import pro.conflux.wallet.ui.adapter.DrawerWalletAdapter;
import pro.conflux.wallet.ui.adapter.TokensAdapter;
import pro.conflux.wallet.utils.BalanceUtils;
import pro.conflux.wallet.utils.LogUtils;
import pro.conflux.wallet.utils.ToastUtils;
import pro.conflux.wallet.utils.WalletDaoUtils;
import pro.conflux.wallet.viewmodel.TokensViewModel;
import pro.conflux.wallet.viewmodel.TokensViewModelFactory;
import com.gyf.barlibrary.ImmersionBar;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.lcodecore.tkrefreshlayout.header.progresslayout.ProgressLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import butterknife.BindView;
import butterknife.OnClick;

import static pro.conflux.wallet.C.EXTRA_ADDRESS;
import static pro.conflux.wallet.C.Key.WALLET;


public class PropertyFragment extends BaseFragment implements View.OnClickListener {

    TokensViewModelFactory tokensViewModelFactory;
    private TokensViewModel tokensViewModel;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.common_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.refreshLayout)
    TwinklingRefreshLayout refreshLayout;

    @BindView(R.id.tv_property_label)
    TextView tvPropertToolbaryLabel;

    @BindView(R.id.drawer)
    DrawerLayout drawer;
    @BindView(R.id.lv_wallet)
    ListView lvWallet;

    private CfxWallet currCfxWallet;

    List<Token> tokenItems;

    private LinearLayoutManager linearLayoutManager;
    private TokensAdapter recyclerAdapter;
    private View headView;

    private int bannerHeight = 300;
    private View mIv;

    @BindView(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefresh;

    private static final int QRCODE_SCANNER_REQUEST = 1100;
    private static final int CREATE_WALLET_REQUEST = 1101;
    private static final int ADD_NEW_PROPERTY_REQUEST = 1102;
    private static final int WALLET_DETAIL_REQUEST = 1104;

    private DrawerWalletAdapter drawerWalletAdapter;

    FetchWalletInteract fetchWalletInteract;

    private TextView tvWalletName;
    private TextView tvWalletAddress;
    private TextView tvTolalAssetValue;
    private TextView tvTolalAsset;

    private String currency;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_property;
    }

    @Override
    public void attachView() {

    }

    @Override
    public void initDatas() {

        linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        //设置布局管理器
        recyclerView.setLayoutManager(linearLayoutManager);

        //设置适配器
        recyclerAdapter = new TokensAdapter(R.layout.list_item_property, new ArrayList<>());  //

        recyclerView.setAdapter(recyclerAdapter);
        recyclerAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {

                Token token = tokenItems.get(position);

                String tokenType = token.tokenInfo.type == null ? "" : token.tokenInfo.type;//获取token的类型

                //根据判断的类型进入不同页面
                switch (tokenType){

                    case "CRC721":
                        Intent crc721_intent = new Intent(getActivity(), TokenidListActivity.class);
                        crc721_intent.putExtra(C.EXTRA_BALANCE, token.balance);
                        crc721_intent.putExtra(C.EXTRA_ADDRESS, currCfxWallet.getAddress());
                        crc721_intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, token.tokenInfo.address);
                        crc721_intent.putExtra(C.EXTRA_CONTRACT_NAME, token.tokenInfo.name);
                        crc721_intent.putExtra(C.EXTRA_CONTRACT_TYPE, token.tokenInfo.type);
                        crc721_intent.putExtra(C.EXTRA_SYMBOL, token.tokenInfo.symbol);
                        crc721_intent.putExtra(C.EXTRA_DECIMALS, token.tokenInfo.decimals);
                        startActivity(crc721_intent);
                        break;
                    default:
                        Intent intent = new Intent(getActivity(), PropertyDetailActivity.class);
                        intent.putExtra(C.EXTRA_BALANCE, token.balance);
                        intent.putExtra(C.EXTRA_ADDRESS, currCfxWallet.getAddress());
                        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, token.tokenInfo.address);
                        intent.putExtra(C.EXTRA_SYMBOL, token.tokenInfo.symbol);
                        intent.putExtra(C.EXTRA_DECIMALS, token.tokenInfo.decimals);
                        startActivity(intent);
                }


            }
        });


        fetchWalletInteract = new FetchWalletInteract();
        fetchWalletInteract.fetch().subscribe(this::showDrawerWallets);

        tokensViewModelFactory = new TokensViewModelFactory();
        tokensViewModel = ViewModelProviders.of(this.getActivity(), tokensViewModelFactory)
                .get(TokensViewModel.class);

        tokensViewModel.defaultWallet().observe(this,  this::showWallet);

//        tokensViewModel.progress().observe(this, systemView::showProgress);
//        tokensViewModel.error().observe(this, this::onError);

        tokensViewModel.tokens().observe(this, this::onTokens);
//        tokensViewModel.prices().observe(this, this::onPrices);//获取价格，暂时注释

        currency = tokensViewModel.getCurrency();
    }

    private void onTokens(Token[] tokens) {
        tokenItems = Arrays.asList(tokens);
        recyclerAdapter.setTokens(tokenItems);
    }

    private void onPrices(Ticker ticker) {
        BigDecimal sum = new BigDecimal(0);

        for (Token token : tokenItems) {
            if (token.tokenInfo.symbol.equals(ticker.symbol)) {
                if (token.balance == null) {
                    token.value = "0";
                } else {
                    token.value = BalanceUtils.cfxToUsd(ticker.price, token.balance);
                }
            }
            if (!TextUtils.isEmpty(token.value)) {
                sum  = sum.add(new BigDecimal(token.value));
            }

        }

        if (tvTolalAssetValue != null) {
            tvTolalAssetValue.setText(sum.setScale(2, RoundingMode.CEILING).toString());
        }

        recyclerAdapter.setTokens(tokenItems);
    }

    private void initTinklingLayoutListener() {
        ViewGroup.LayoutParams bannerParams = mIv.getLayoutParams();
        ViewGroup.LayoutParams titleBarParams = mToolbar.getLayoutParams();
        bannerHeight = bannerParams.height - titleBarParams.height - ImmersionBar.getStatusBarHeight(getActivity());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int totalDy = 0;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalDy += dy;

                ImmersionBar immersionBar = ImmersionBar.with(PropertyFragment.this)
                        .addViewSupportTransformColor(mToolbar, R.color.colorPrimary);


                if (totalDy <= bannerHeight) {
                    float alpha = (float) totalDy / bannerHeight;
                    immersionBar.statusBarAlpha(alpha)
                            .init();
                    // 设置资产文字alpha值
                    if (totalDy >= bannerHeight / 2) {
                        float tvPropertyAlpha = (float) (totalDy - bannerHeight / 2) / (bannerHeight / 2);
                        tvPropertToolbaryLabel.setAlpha(tvPropertyAlpha);
                        int top = (int) (mToolbar.getHeight() - mToolbar.getHeight() * alpha);
                        tvPropertToolbaryLabel.setPadding(0, top, 0, 0);
                    } else {
                        tvPropertToolbaryLabel.setPadding(0, mToolbar.getHeight(), 0, 0);
                        tvPropertToolbaryLabel.setAlpha(0);
                    }
                } else {
                    immersionBar.statusBarAlpha(1.0f).init();
                    tvPropertToolbaryLabel.setAlpha(1.0f);
                }
            }
        });
        refreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(final TwinklingRefreshLayout refreshLayout) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.finishRefreshing();
                        mToolbar.setVisibility(View.VISIBLE);
                        ImmersionBar.with(PropertyFragment.this).statusBarDarkFont(false).init();
                    }
                }, 2000);
            }

            @Override
            public void onPullingDown(TwinklingRefreshLayout refreshLayout, float fraction) {
                mToolbar.setVisibility(View.GONE);
                ImmersionBar.with(PropertyFragment.this).statusBarDarkFont(true).init();
            }

            @Override
            public void onPullDownReleasing(TwinklingRefreshLayout refreshLayout, float fraction) {
                if (Math.abs(fraction - 1.0f) > 0) {
                    mToolbar.setVisibility(View.VISIBLE);
                    ImmersionBar.with(PropertyFragment.this).statusBarDarkFont(false).init();
                } else {
                    mToolbar.setVisibility(View.GONE);
                    ImmersionBar.with(PropertyFragment.this).statusBarDarkFont(true).init();
                }
            }
        });
    }

    private void addHeaderView() {
        ProgressLayout headerView = new ProgressLayout(getContext());
        refreshLayout.setHeaderView(headerView);
        headView = LayoutInflater.from(getContext()).inflate(R.layout.list_header_item, (ViewGroup) recyclerView.getParent(), false);
        mIv = headView.findViewById(R.id.iv);
        headView.findViewById(R.id.lly_add_token).setOnClickListener(this);
        headView.findViewById(R.id.lly_wallet_address).setOnClickListener(this);
        headView.findViewById(R.id.civ_wallet_logo).setOnClickListener(this);
        tvWalletName = (TextView) headView.findViewById(R.id.tv_wallet_name);
        tvWalletAddress =(TextView) headView.findViewById(R.id.tv_wallet_address);
        tvTolalAssetValue = (TextView) headView.findViewById(R.id.tv_total_value);

        tvTolalAsset = (TextView) headView.findViewById(R.id.tv_total_assets);
        if (currency.equals("CNY")) {
            tvTolalAsset.setText(R.string.property_total_assets_cny);
        } else {
            tvTolalAsset.setText(R.string.property_total_assets_usd);
        }


        recyclerAdapter.addHeaderView(headView);
    }

    @Override
    public void onResume() {
        super.onResume();

        ImmersionBar.with(this)
                .titleBar(mToolbar, false)
                .navigationBarColor(R.color.colorPrimary)
                .init();

        tokensViewModel.prepare();

        // 更改货币单位
        if (!currency.equals(tokensViewModel.getCurrency())) {
            currency = tokensViewModel.getCurrency();
            if (currency.equals("CNY")) {
                tvTolalAsset.setText(R.string.property_total_assets_cny);
            } else {
                tvTolalAsset.setText(R.string.property_total_assets_usd);
            }
        }

    }


    @Override
    public void configViews() {
        addHeaderView();
        initTinklingLayoutListener();
        drawer.setScrimColor(getContext().getResources().getColor(R.color.property_drawer_scrim_bg_color));
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    @OnClick({R.id.lly_menu, R.id.lly_qrcode_scanner, R.id.lly_create_wallet})
    public void onClick(View view) {
        Intent intent = null;
        CfxWallet wallet = null;
        switch (view.getId()) {
            case R.id.lly_menu:
                openOrCloseDrawerLayout();
                break;
            case R.id.lly_qrcode_scanner:// 二维码扫描
                intent = new Intent(mContext, QRCodeScannerActivity.class);
                startActivityForResult(intent, QRCODE_SCANNER_REQUEST);
                openOrCloseDrawerLayout();
                break;
            case R.id.lly_create_wallet:// 创建钱包
                intent = new Intent(mContext, CreateWalletActivity.class);
                startActivityForResult(intent, CREATE_WALLET_REQUEST);
                openOrCloseDrawerLayout();
                break;
            case R.id.lly_add_token:// 跳转添加资产
//                tokensViewModel.showAddToken(this.getApplicationContext());

                intent = new Intent(mContext, AddTokenActivity.class);
                intent.putExtra(WALLET,  currCfxWallet.getAddress());
                startActivityForResult(intent, ADD_NEW_PROPERTY_REQUEST);

                break;

            case R.id.lly_wallet_address:  // 跳转收款码
                intent = new Intent(mContext, GatheringQRCodeActivity.class);
                wallet = WalletDaoUtils.getCurrent();
                if (wallet == null) {
                    return;
                }

                intent.putExtra(EXTRA_ADDRESS, wallet.getAddress());
                startActivity(intent);
                break;

            case R.id.civ_wallet_logo:// 跳转钱包详情
                intent = new Intent(mContext, WalletDetailActivity.class);
                wallet = WalletDaoUtils.getCurrent();
                if (wallet == null) {
                    return;
                }
                intent.putExtra("walletId", wallet.getId());
                intent.putExtra("walletPwd", wallet.getPassword());
                intent.putExtra("walletAddress", wallet.getAddress());
                intent.putExtra("walletName", wallet.getName());
                intent.putExtra("walletMnemonic", wallet.getMnemonic());
                intent.putExtra("walletIsBackup", wallet.getIsBackup());
                startActivityForResult(intent, WALLET_DETAIL_REQUEST);
                break;

        }

    }

    // 打开关闭DrawerLayout
    private void openOrCloseDrawerLayout() {
        boolean drawerOpen = drawer.isDrawerOpen(Gravity.END);
        if (drawerOpen) {
            drawer.closeDrawer(Gravity.END);
        } else {
            drawer.openDrawer(Gravity.END);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QRCODE_SCANNER_REQUEST) {
            if (data != null) {
                String scanResult = data.getStringExtra("scan_result");
                // 对扫描结果进行处理
                ToastUtils.showLongToast(scanResult);

                Intent intent = new Intent(mContext, SendActivity.class);
                intent.putExtra("scan_result", scanResult );

                startActivity(intent);

            }
        } else if (requestCode == WALLET_DETAIL_REQUEST) {
            if (data != null) {
//                mPresenter.loadAllWallets();
                startActivity(new Intent(mContext, WalletMangerActivity.class));
            }
        }
    }

    public void showWallet(CfxWallet wallet) {
        currCfxWallet = wallet;
        tvWalletName.setText(wallet.getName());
        tvWalletAddress.setText(wallet.getAddress());

        //       openOrCloseDrawerLayout();
    }

    public void showDrawerWallets(final List<CfxWallet> cfxWallets) {
//        for (int i = 0; i < cfxWallets.size(); i++) {
//            LogUtils.i("PropertyFragment", "Cfxwallets" + cfxWallets.get(i).toString());
//        }
        drawerWalletAdapter = new DrawerWalletAdapter(getContext(), cfxWallets, R.layout.list_item_drawer_property_wallet);
        lvWallet.setAdapter(drawerWalletAdapter);
        lvWallet.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                drawerWalletAdapter.setCurrentWalletPosition(position);

                CfxWallet wallet = drawerWalletAdapter.getDatas().get(position);

                tokensViewModel.updateDefaultWallet(wallet.getId());

                openOrCloseDrawerLayout();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return super.onCreateView(inflater, container, state);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ImmersionBar.with(this).destroy();
    }
}
