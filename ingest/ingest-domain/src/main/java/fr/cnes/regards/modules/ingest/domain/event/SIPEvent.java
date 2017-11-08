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
package fr.cnes.regards.modules.ingest.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * AMQP Event to inform system for all {@link SIPEntity} state modification
 * @author Sébastien Binda
 */
@Event(target = Target.ALL)
public class SIPEvent implements ISubscribable {

    private SIPState state;

    private String ipId;

    private SIPEvent() {
    }

    public SIPEvent(SIPEntity sip) {
        this.state = sip.getState();
        this.ipId = sip.getIpId();
    }

    public SIPState getState() {
        return state;
    }

    public String getIpId() {
        return ipId;
    }

}