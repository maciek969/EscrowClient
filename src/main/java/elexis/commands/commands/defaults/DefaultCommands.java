/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package elexis.commands.commands.defaults;

import elexis.commands.ICommand;
import elexis.Elexis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {

    public static List<ICommand> createAll() {
        Elexis mainclass = Elexis.getInstance();
        List<ICommand> commands = new ArrayList<>(Arrays.asList(
                new HelpCommand(mainclass),
                new ConfigCommand(mainclass),
                new FriendCommand(mainclass),
                new GpsCommand(mainclass),
                new StaffCommand(mainclass),
                new BindCommand(mainclass),

                new BlockESPCommand(),
                new AutoFarmCommand(),
                new RCTCommand(mainclass),
                new PrefixCommand()
        ));
        return Collections.unmodifiableList(commands);
    }
}
