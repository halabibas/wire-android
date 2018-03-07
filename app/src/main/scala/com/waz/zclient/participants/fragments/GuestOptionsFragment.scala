/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.participants.fragments

import android.content.{ClipData, ClipboardManager, Context, DialogInterface}
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.support.v7.widget.SwitchCompat
import android.view.animation.{AlphaAnimation, Animation}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{CompoundButton, LinearLayout, TextView}
import com.waz.ZLog.ImplicitTag._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.common.views.MenuRowButton
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils.showToast
import com.waz.zclient.utils.{RichView, ViewUtils}
import com.waz.zclient.{FragmentHelper, R}

class GuestOptionsFragment extends FragmentHelper {

  import Threading.Implicits.Background

  implicit def cxt: Context = getActivity

  private lazy val zms = inject[Signal[ZMessaging]]

  private lazy val convCtrl = inject[ConversationController]

  //TODO look into using something more similar to SwitchPreference
  private lazy val guestsSwitch = returning(view[SwitchCompat](R.id.guest_toggle)) { vh =>
    convCtrl.currentConvIsTeamOnly.currentValue.foreach(teamOnly => vh.foreach(_.setChecked(!teamOnly)))
    convCtrl.currentConvIsTeamOnly.onUi(teamOnly => vh.foreach(_.setChecked(!teamOnly)))
  }

  private lazy val guestsTitle = view[TextView](R.id.guest_toggle_title)
  private lazy val guestLinkText = returning(view[TypefaceTextView](R.id.link_button_link_text)) { text =>
    convCtrl.currentConv.map(_.link).onUi {
      case Some(link) =>
        text.foreach(_.setVisibility(View.VISIBLE))
        text.foreach(_.setText(link.url))
      case None =>
        text.foreach(_.setVisibility(View.GONE))
    }
  }
  private lazy val guestLinkCreate = returning(view[LinearLayout](R.id.link_button_create_link)) { text =>
    convCtrl.currentConv.map(_.link.isDefined).onUi { hasLink =>
      text.foreach(_.setVisibility(if (hasLink) View.GONE else View.VISIBLE))
    }
  }
  private lazy val copyLinkButton = returning(view[MenuRowButton](R.id.copy_link_button)) { button =>
    convCtrl.currentConv.map(_.link.isDefined).onUi { hasLink =>
      button.foreach(_.setVisibility(if (hasLink) View.VISIBLE else View.GONE))
    }
  }
  private lazy val shareLinkButton = returning(view[MenuRowButton](R.id.share_link_button)) { button =>
    convCtrl.currentConv.map(_.link.isDefined).onUi { hasLink =>
      button.foreach(_.setVisibility(if (hasLink) View.VISIBLE else View.GONE))
    }
  }
  private lazy val revokeLinkButton = returning(view[MenuRowButton](R.id.revoke_link_button)) { button =>
    convCtrl.currentConv.map(_.link.isDefined).onUi { hasLink =>
      button.foreach(_.setVisibility(if (hasLink) View.VISIBLE else View.GONE))
    }
  }
  private lazy val guestLinkOptions = returning(view[ViewGroup](R.id.guest_link_options)) { linkOptions =>
    convCtrl.currentConv.map(_.isTeamOnly).onUi { isTeamOnly =>
      linkOptions.foreach(_.setVisibility(if (isTeamOnly) View.GONE else View.VISIBLE))
    }
  }

  // This is a workaround for the bug where child fragments disappear when
  // the parent is removed (as all children are first removed from the parent)
  // See https://code.google.com/p/android/issues/detail?id=55228
  // Apply the workaround only if this is a child fragment, and the parent is being removed.
  override def onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation =
    Option(getParentFragment) match {
      case Some(parent) if !enter && parent.isRemoving =>
        returning(new AlphaAnimation(1, 1)) {
          _.setDuration(ViewUtils.getNextAnimationDuration(parent))
        }
      case _ => super.onCreateAnimation(transit, enter, nextAnim)
    }


  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.guest_options_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    guestsSwitch
    guestLinkText.foreach(linkText => linkText.onClick(copyToClipboard(linkText.getText.toString)))
    guestLinkCreate.foreach(_.onClick {
      (for {
        zms <- zms.head
        conv <- convCtrl.currentConv.head
        link <- zms.conversations.createLink(conv.id)
      } yield link).map {
        case Left(_) => showToast(R.string.allow_guests_error_title)
        case _ =>
      } (Threading.Ui)
    })

    copyLinkButton.foreach(_.onClick(convCtrl.currentConv.head.map(_.link.foreach(link => copyToClipboard(link.url)))(Threading.Ui)))
    shareLinkButton.foreach(_.onClick {
      convCtrl.currentConv.head.map(_.link.foreach { link =>
        val intentBuilder = ShareCompat.IntentBuilder.from(getActivity)
        //intentBuilder.setChooserTitle(R.string.conversation__action_mode__fwd__chooser__title)
        intentBuilder.setType("text/plain")
        intentBuilder.setText(link.url)
        intentBuilder.startChooser()
      })(Threading.Ui)
    })
    revokeLinkButton.foreach(_.onClick {

      ViewUtils.showAlertDialog(getContext,
        R.string.empty_string,
        R.string.revoke_link_message,
        R.string.revoke_link_confirm,
        android.R.string.cancel,
        new DialogInterface.OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = {
            (for {
              zms <- zms.head
              conv <- convCtrl.currentConv.head
              res <- zms.conversations.removeLink(conv.id)
            } yield res).map {
              case Left(_) => showToast(R.string.allow_guests_error_title)
              case _ =>
            } (Threading.Ui)
            dialog.dismiss()
          }
        }, new DialogInterface.OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = dialog.dismiss()
        })
    })
    guestLinkOptions
  }

  private def copyToClipboard(text: String): Unit = {
    val clipboard: ClipboardManager = getContext.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
    val clip: ClipData = ClipData.newPlainText("", text) //TODO: label?
    clipboard.setPrimaryClip(clip)
    showToast(R.string.link_copied_toast)
  }

  override def onResume() = {
    super.onResume()
    guestsSwitch.foreach {
      _.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener {
        override def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit = {

          def setTeamOnly(): Unit = {
            (for {
              z <- zms.head
              c <- convCtrl.currentConvId.head
              resp <- z.conversations.setToTeamOnly(c, !isChecked)
            } yield resp).map { resp =>
              setGuestsSwitchEnabled(true)
              resp match {
                case Right(_) => //
                case Left(_) =>
                  ViewUtils.showAlertDialog(getContext, R.string.allow_guests_error_title, R.string.allow_guests_error_body, android.R.string.ok, new DialogInterface.OnClickListener {
                    override def onClick(dialog: DialogInterface, which: Int): Unit = dialog.dismiss()
                  }, true)
              }
            }(Threading.Ui)
          }

          setGuestsSwitchEnabled(false)

          if (!isChecked) {
            ViewUtils.showAlertDialog(getContext,
              R.string.empty_string,
              R.string.allow_guests_warning_body,
              R.string.allow_guests_warning_confirm,
              android.R.string.cancel,
              new DialogInterface.OnClickListener {
                override def onClick(dialog: DialogInterface, which: Int): Unit = {
                  setTeamOnly()
                  dialog.dismiss()
                }
              }, new DialogInterface.OnClickListener {
                override def onClick(dialog: DialogInterface, which: Int): Unit = {
                  guestsSwitch.setChecked(true)
                  setGuestsSwitchEnabled(true)
                  dialog.dismiss()
                }
              })
          } else {
            setTeamOnly()
          }
        }
      })
    }
  }

  private def setGuestsSwitchEnabled(enabled: Boolean) = {
    guestsSwitch.foreach(_.setEnabled(enabled))
    guestsTitle.foreach(_.setAlpha(if (enabled) 1f else 0.5f))
  }

  override def onStop() = {
    guestsSwitch.foreach(_.setOnCheckedChangeListener(null))
    super.onStop()
  }

  override def onBackPressed(): Boolean = {
    super.onBackPressed()
    getFragmentManager.popBackStack
    true
  }
}

object GuestOptionsFragment {
  val Tag = implicitLogTag
}
