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
package com.waz.zclient.conversation

import android.os.Bundle
import android.support.v7.widget.{LinearLayoutManager, RecyclerView, Toolbar}
import android.view.{LayoutInflater, View, ViewGroup}
import android.view.animation.{AlphaAnimation, Animation}
import com.waz.model.MessageId
import com.waz.service.ZMessaging
import com.waz.utils.events.{Signal, Subscription}
import com.waz.utils.returning
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.conversation.ConversationManagerFragment
import com.waz.zclient.{FragmentHelper, R}
import com.waz.zclient.utils.ViewUtils

class LikesListFragment extends BaseFragment[LikesListFragment.Container] with FragmentHelper {

  private lazy val toolbar        = view[Toolbar](R.id.t__likes_list__toolbar)
  private lazy val likersListView = view[RecyclerView](R.id.rv__likes_list)
  private lazy val likedMessage   = MessageId(getArguments.getString(LikesListFragment.ArgumentLikedMessage))
  private lazy val likesAdapter   = new LikesAdapter(getContext)

  private var sub = Option.empty[Subscription]

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

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_likes_list, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)

    likersListView.foreach { view =>
      view.setLayoutManager(new LinearLayoutManager(getActivity))
      view.setAdapter(likesAdapter)
    }

    toolbar

    sub = Option((for {
      msgsAndLikes <- inject[Signal[ZMessaging]].map(_.msgAndLikes)
      Some(likes)  <- Signal.future(msgsAndLikes.getMessageAndLikes(likedMessage))
    } yield likes.likes).onUi { likes =>
      likesAdapter.setLikes(likes.toSet)
    })
  }

  override def onResume(): Unit = {
    super.onResume()

    toolbar.foreach(_.setNavigationOnClickListener(new View.OnClickListener {
      override def onClick(v: View): Unit = onBackPressed()
    }))
  }

  override def onPause(): Unit = {
    toolbar.foreach(_.setNavigationOnClickListener(null))

    super.onPause()
  }

  override def onDestroyView(): Unit = {
    sub.foreach(_.destroy())
    sub = None

    super.onDestroyView()
  }

  override def onBackPressed(): Boolean = Option(getParentFragment) match {
    case Some(f: ConversationManagerFragment) =>
      f.closeLikesList()
      true
    case _ => false
  }
}

object LikesListFragment {
  val Tag: String = classOf[LikesListFragment].getName
  val ArgumentLikedMessage = "ARGUMENT_LIKED_MESSAGE"

  def newInstance(messageId: MessageId): LikesListFragment = returning(new LikesListFragment){
    _.setArguments(returning(new Bundle){
      _.putString(ArgumentLikedMessage, messageId.str)
    })
  }

  trait Container {}
}
