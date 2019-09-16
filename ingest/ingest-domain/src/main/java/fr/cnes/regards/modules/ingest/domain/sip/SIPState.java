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
package fr.cnes.regards.modules.ingest.domain.sip;

/**
 * SIP lifecycle
 *
 * @author Marc Sordi
 */
public enum SIPState implements ISipState {

    /**
     * SIP has been properly ingested
     */
    INGESTED,
    /**
     * SIP has been properly stored
     */
    STORED,
    /**
     * SIP is deleted but we still keep it in database
     */
    DELETED,
    /**
     * SIP has encountered an issue
     */
    ERROR;

    @Override
    public String getName() {
        return this.name();
    }
}