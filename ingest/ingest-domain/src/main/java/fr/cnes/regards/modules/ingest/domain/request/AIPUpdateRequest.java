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
package fr.cnes.regards.modules.ingest.domain.request;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTask;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Keep info about an AIP update request
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_update_request",
        indexes = { @Index(name = "idx_update_request_search", columnList = "session_owner,session_name,state"),
                @Index(name = "idx_update_request_state", columnList = "state") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPUpdateRequest extends AbstractInternalRequest {

    @Id
    @SequenceGenerator(name = "updateRequestSequence", initialValue = 1, sequenceName = "seq_update_request")
    @GeneratedValue(generator = "updateRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    @Column(length = 128, name = "session_owner", nullable = false)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Column(length = 128, name = "session_name", nullable = false)
    private String session;

    @OneToOne
    @JoinColumn(name = "update_task_id", foreignKey = @ForeignKey(name = "fk_update_request_update_task_id"))
    @Valid
    private AIPUpdateTask updateTask;

    /**
     * AIP to update
     */
    @ManyToOne
    @JoinColumn(name = "aip_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_update_request_aip"))
    private AIPEntity aip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getSessionOwner() {
        return sessionOwner;
    }

    @Override
    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    @Override
    public String getSession() {
        return session;
    }

    @Override
    public void setSession(String session) {
        this.session = session;
    }

    public AIPEntity getAip() {
        return aip;
    }

    public void setAip(AIPEntity aip) {
        this.aip = aip;
    }

    public AIPUpdateTask getUpdateTask() {
        return updateTask;
    }

    public void setUpdateTask(AIPUpdateTask updateTask) {
        this.updateTask = updateTask;
    }

    public static AIPUpdateRequest build(AIPEntity aip, AIPUpdateTask updateTask) {
        AIPUpdateRequest updateRequest = new  AIPUpdateRequest();
        updateRequest.setUpdateTask(updateTask);
        updateRequest.setAip(aip);
        updateRequest.setSessionOwner(aip.getIngestMetadata().getSessionOwner());
        updateRequest.setSession(aip.getIngestMetadata().getSession());
        return updateRequest;
    }
}
