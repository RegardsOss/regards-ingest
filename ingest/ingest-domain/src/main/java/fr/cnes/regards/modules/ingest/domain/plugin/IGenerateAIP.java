/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.domain.plugin;

import java.util.List;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Third <b>required</b> step of the SIP processing chain.
 *
 * @author Marc Sordi
 *
 */
@FunctionalInterface
@PluginInterface(description = "AIP generation plugin contract")
public interface IGenerateAIP {

    /**
     * Generate one or more {@link AIP} from passed {@link SIP}.
     * @param sip {@link SIP}
     * @return generated {@link AIP}
     */
    List<AIP> generate(SIP sip);
}
