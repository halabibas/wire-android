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
package com.waz.zclient.pages.main.pickuser.controller;

import com.waz.model.UserId;

public interface IPickUserController {

    enum Destination {
        CONVERSATION_LIST,
        PARTICIPANTS,
        CURSOR
    }

    void addPickUserScreenControllerObserver(PickUserControllerScreenObserver observer);

    void removePickUserScreenControllerObserver(PickUserControllerScreenObserver observer);

    // Showing people picker
    void showPickUser(Destination destination);

    /**
     * @return true, if a picker was hidden, false otherwise
     */
    boolean hidePickUser(Destination destination);

    boolean isHideWithoutAnimations();

    void hidePickUserWithoutAnimations(Destination destination);

    boolean isShowingPickUser(Destination destination);

    void resetShowingPickUser(Destination destination);

    void showUserProfile(UserId userId);

    void hideUserProfile();

    boolean isShowingUserProfile();

    void tearDown();
}
