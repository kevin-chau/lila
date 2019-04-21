package views.html
package round

import chess.variant.{ Variant, Crazyhouse }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Pov, Player }

import controllers.routes

object bits {

  def layout(
    variant: Variant,
    title: String,
    moreJs: Frag = emptyFrag,
    openGraph: Option[lila.app.ui.OpenGraph] = None,
    moreCss: Frag = emptyFrag,
    chessground: Boolean = true,
    playing: Boolean = false,
    robots: Boolean = false
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = frag(
        cssTag { if (variant == Crazyhouse) "round.zh" else "round" },
        ctx.blind option cssTag("round.nvui"),
        moreCss
      ),
      chessground = chessground,
      playing = playing,
      robots = robots,
      deferJs = true,
      zoomable = true
    )(body)

  def crosstable(cross: Option[lila.game.Crosstable.WithMatchup], game: Game)(implicit ctx: Context) =
    cross map { c =>
      views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)
    }

  def underchat(game: Game)(implicit ctx: Context) = frag(
    div(
      cls := "chat__members none",
      aria.live := "off",
      aria.relevant := "additions removals text"
    )(
        span(cls := "number")(nbsp),
        " ",
        trans.spectators.txt().replace(":", ""),
        " ",
        span(cls := "list")
      ),
    isGranted(_.ViewBlurs) option div(cls := "round__mod")(
      game.players.filter(p => game.playerBlurPercent(p.color) > 30) map { p =>
        div(
          playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, withOnline = false, mod = true),
          s"${p.blurs.nb}/${game.playerMoves(p.color)} blurs",
          strong(game.playerBlurPercent(p.color), "%")
        )
      },
      game.players flatMap { p => p.holdAlert.map(p ->) } map {
        case (p, h) => div(
          playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
          "hold alert",
          br,
          s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
        )
      }
    )
  )

  def others(playing: List[Pov], simul: Option[lila.simul.Simul])(implicit ctx: Context) = frag(
    h3(
      simul.map { s =>
        span(cls := "simul")(
          a(href := routes.Simul.show(s.id))("SIMUL"),
          span(cls := "win")(s.wins, " W"), " / ",
          span(cls := "draw")(s.draws, " D"), " / ",
          span(cls := "loss")(s.losses, " L"), " / ",
          s.ongoing, " ongoing"
        )
      } getOrElse trans.currentGames.frag(),
      "round-toggle-autoswitch" |> { id =>
        span(cls := "move-on switcher", st.title := trans.automaticallyProceedToNextGameAfterMoving.txt())(
          label(`for` := id)(trans.autoSwitch.frag()),
          span(cls := "switch")(
            input(st.id := id, cls := "cmn-toggle", tpe := "checkbox"),
            label(`for` := id)
          )
        )
      }
    ),
    div(cls := "now-playing")(
      playing.partition(_.isMyTurn) |> {
        case (myTurn, otherTurn) =>
          (myTurn ++ otherTurn.take(6 - myTurn.size)) take 9 map { pov =>
            a(href := routes.Round.player(pov.fullId), cls := pov.isMyTurn.option("my_turn"))(
              gameFen(pov, withLink = false, withTitle = false, withLive = false),
              span(cls := "meta")(
                playerText(pov.opponent, withRating = false),
                span(cls := "indicator")(
                  if (pov.isMyTurn) pov.remainingSeconds.fold(trans.yourTurn())(secondsFromNow(_, true))
                  else nbsp
                )
              )
            )
          }
      }
    )
  )

  private[round] def side(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lila.tournament.TourMiniView],
    simul: Option[lila.simul.Simul],
    userTv: Option[lila.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context) = views.html.game.side(
    pov,
    (data \ "game" \ "initialFen").asOpt[String].map(chess.format.FEN),
    tour.map(_.tour),
    simul = simul,
    userTv = userTv,
    bookmarked = bookmarked
  )

  def roundAppPreload(pov: Pov, controls: Boolean)(implicit ctx: Context) =
    div(cls := "round__app")(
      div(cls := "round__app__board main-board")(board.bits.domPreload(pov.some)),
      div(cls := "round__app__table"),
      div(cls := "ruser ruser-top user-link")(i(cls := "line"), a(cls := "text")(playerText(pov.opponent))),
      div(cls := "ruser ruser-bottom user-link")(i(cls := "line"), a(cls := "text")(playerText(pov.player))),
      div(cls := "rclock rclock-top preload")(div(cls := "time")(nbsp)),
      div(cls := "rclock rclock-bottom preload")(div(cls := "time")(nbsp)),
      div(cls := "rmoves")(div(cls := "moves")),
      controls option div(cls := "rcontrols")(i(cls := "ddloader"))
    )
}
