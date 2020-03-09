/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request.update;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.Valid;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;

/**
 * Keep info about an AIP update request
 * @author Léo Mieulet
 */
@Entity(name = RequestTypeConstant.UPDATE_VALUE)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPUpdateRequest extends AbstractRequest {

    //CascadeType.DELETE is not effective with @ManyToOne, so lets set all cascaded operation
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinColumn(name = "update_task_id", foreignKey = @ForeignKey(name = "fk_update_request_update_task_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Valid
    private AbstractAIPUpdateTask updateTask;

    /**
     * AIP to update
     */
    @ManyToOne
    @JoinColumn(name = "aip_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_update_request_aip"))
    private AIPEntity aip;

    public AbstractAIPUpdateTask getUpdateTask() {
        return updateTask;
    }

    public void setUpdateTask(AbstractAIPUpdateTask updateTask) {
        this.updateTask = updateTask;
    }

    public AIPEntity getAip() {
        return aip;
    }

    public void setAip(AIPEntity aip) {
        this.aip = aip;
    }

    public static List<AIPUpdateRequest> build(AIPEntity aip, AIPUpdateParametersDto updateTaskDto, boolean pending) {
        return AIPUpdateRequest.build(aip, AbstractAIPUpdateTask.build(updateTaskDto), pending);
    }

    public static List<AIPUpdateRequest> build(AIPEntity aip, Collection<AbstractAIPUpdateTask> updateTasks,
            boolean pending) {
        List<AIPUpdateRequest> result = new ArrayList<>();
        for (AbstractAIPUpdateTask updateTask : updateTasks) {
            AIPUpdateRequest updateRequest = new AIPUpdateRequest();
            updateRequest.setUpdateTask(updateTask);
            updateRequest.setAip(aip);
            updateRequest.setCreationDate(OffsetDateTime.now());
            updateRequest.setSessionOwner(aip.getSessionOwner());
            updateRequest.setSession(aip.getSession());
            updateRequest.setProviderId(aip.getProviderId());
            updateRequest.setDtype(RequestTypeConstant.UPDATE_VALUE);
            if (pending) {
                updateRequest.setState(InternalRequestState.BLOCKED);
            } else {
                updateRequest.setState(InternalRequestState.TO_SCHEDULE);
            }
            result.add(updateRequest);
        }
        return result;
    }
}
