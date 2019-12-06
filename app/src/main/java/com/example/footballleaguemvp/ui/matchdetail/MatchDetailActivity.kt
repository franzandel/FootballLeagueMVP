package com.example.footballleaguemvp.ui.matchdetail

import android.database.sqlite.SQLiteConstraintException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.footballleaguemvp.R
import com.example.footballleaguemvp.data.Match
import com.example.footballleaguemvp.data.Team
import com.example.footballleaguemvp.utils.ActivityNavigation
import com.example.footballleaguemvp.utils.databasehelper.database
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_match_detail.*
import kotlinx.android.synthetic.main.toolbar_activity.*
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.toast

class MatchDetailActivity : AppCompatActivity(), MatchDetailContract.View {

    companion object {
        const val TAG_MATCH_ID = "matchId"
        const val TAG_MATCH_NAME = "matchName"
    }

    private lateinit var mPresenter: MatchDetailPresenter
    private lateinit var mActivityNavigation: ActivityNavigation
    private lateinit var match: Match
    private var isFavoriteMatch = false
    private var matchId = ""
    private var matchName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_detail)
        val bundle = intent.extras
        if (bundle!=null){
            matchId = bundle.getString(TAG_MATCH_ID, "")
            matchName = bundle.getString(TAG_MATCH_NAME, "")
        }
        setupUi()
        setupClickListener()
        initializeData()
    }

    override fun setupUi() {
        setupToolbar(matchName)
        setupPresenter()
        setupNavigation()
        toggleFavoriteIcon()
    }

    override fun setupPresenter() {
        mPresenter = MatchDetailPresenter(this)
    }

    override fun setupToolbar(title: String) {
        btnToolbarBack.visibility = View.VISIBLE
        tvToolbarTitle.text = matchName
        btnFavorite.visibility = View.VISIBLE
    }

    override fun setupNavigation() {
        mActivityNavigation = ActivityNavigation(this)
    }

    override fun setupClickListener() {
        btnFavorite.setOnClickListener {
            if (isFavoriteMatch){
                removeMatchToFavorite(match)
            } else {
                addMatchToFavorite(match)
            }
        }

        btnToolbarBack.setOnClickListener { onBackPressed() }

        btnFavoriteList.setOnClickListener {
            onBackPressed()
        }
    }

    override fun initializeData() {
        mPresenter.getMatchDetail(matchId)
    }

    override fun displayMatchDetail(match: Match, teamHomeId: String, teamAwayId: String) {
        this.match = match
        mPresenter.getTeamDetail(teamHomeId, teamAwayId)
        tvDate.text = match.dateEvent
        tvTime.text = match.strTime
        tvHomeScore.text = match.intHomeScore ?: "-"
        tvAwayScore.text = match.intAwayScore ?: "-"
        tvHomeTeam.text = match.strHomeTeam
        tvAwayTeam.text = match.strAwayTeam
    }

    override fun displayHomeTeamDetail(team: Team) {
        Picasso.get().load(team.strTeamLogo).placeholder(resources.getDrawable(R.drawable.loading_animation)).into(ivHomeLogo)
    }

    override fun displayAwayTeamDetail(team: Team) {
        Picasso.get().load(team.strTeamLogo).placeholder(resources.getDrawable(R.drawable.loading_animation)).into(ivAwayLogo)
    }

    override fun addMatchToFavorite(match: Match) {
        try {
            database.use {
                insert(Match.TABLE_FAVORITE_MATCH,
                    Match.EVENT_ID to match.idEvent,
                    Match.EVENT_NAME to match.strEvent,
                    Match.EVENT_HOME_TEAM to match.strHomeTeam,
                    Match.EVENT_AWAY_TEAM to match.strAwayTeam,
                    Match.EVENT_HOME_SCORE to match.intHomeScore,
                    Match.EVENT_AWAY_SCORE to match.intAwayScore,
                    Match.EVENT_DATE to match.dateEvent,
                    Match.EVENT_TIME to match.strTime,
                    Match.EVENT_ID_HOME_TEAM to match.idHomeTeam,
                    Match.EVENT_ID_AWAY_TEAM to match.idAwayTeam,
                    Match.EVENT_SPORT_NAME to match.strSport)
            }
            toast("${match.strEvent} has been added to favorite list").show()
            toggleFavoriteIcon()
        } catch (e: SQLiteConstraintException){
            toast("Fail to add match to favorite list").show()
        }
    }

    override fun removeMatchToFavorite(match: Match) {
        try {
            database.use {
                delete(Match.TABLE_FAVORITE_MATCH, "(${Match.EVENT_ID} = {idEvent})",
                    "idEvent" to matchId)
            }
            toast("${match.strEvent} has been removed from favorite list").show()
            toggleFavoriteIcon()
        } catch (e: SQLiteConstraintException){
            toast("Fail to remove match from favorite list").show()
        }
    }

    override fun toggleFavoriteIcon() {
        database.use {
            val result = select(Match.TABLE_FAVORITE_MATCH)
                .whereArgs("(${Match.EVENT_ID} = {idEvent})",
                    "idEvent" to matchId)
            val favorite = result.parseList(classParser<Match>())
            isFavoriteMatch = favorite.isNotEmpty()
        }

        if (isFavoriteMatch){
            btnFavorite.setImageResource(R.drawable.ic_favorite_full)
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
        }
    }
}
