/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

/**
 * Default AIP generation plugin. The plugin automatically build a single AIP based on SIP information.
 *
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team", description = "Default single AIP generation", id = "DefaultSingleAIPGeneration",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class DefaultSingleAIPGeneration implements IAipGeneration {

    @Override
    public List<AIP> generate(SIP sip, UniformResourceName aipId, UniformResourceName sipId, String providerId) {
        List<AIP> aips = new ArrayList<>();
        aips.add(AIP.build(sip, aipId, Optional.of(sipId), providerId));
        return aips;
    }
}