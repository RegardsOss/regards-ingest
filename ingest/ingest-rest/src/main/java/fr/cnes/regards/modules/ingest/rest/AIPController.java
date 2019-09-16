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
package fr.cnes.regards.modules.ingest.rest;


import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller manages AIP.
 *
 * @author Léo Mieulet
 * @author Marc Sordi
 */
@RestController
@RequestMapping(AIPController.TYPE_MAPPING)
public class AIPController implements IResourceController<AIPEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPController.class);

    public static final String TYPE_MAPPING = "/aips";

    public static final String REQUEST_PARAM_STATE = "state";
    public static final String REQUEST_PARAM_FROM = "from";
    public static final String REQUEST_PARAM_TO = "to";
    public static final String REQUEST_PARAM_TAGS = "tags";
    public static final String REQUEST_PARAM_PROVIDER_ID = "providerId";
    public static final String REQUEST_PARAM_SESSION_OWNER = "sessionOwner";
    public static final String REQUEST_PARAM_SESSION = "session";
    public static final String REQUEST_PARAM_CATEGORIES = "categories";
    public static final String REQUEST_PARAM_STORAGES = "storages";

    public static final String AIP_ID_PATH_PARAM = "aip_id";

    public static final String AIP_DOWNLOAD_PATH = "/{" + AIP_ID_PATH_PARAM + "}/download";

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPService aipService;
    /**
     * Retrieve a page of aip metadata according to the given parameters
     * @param state state the aips should be in
     * @param from date(UTC) after which the aip should have been added to the system
     * @param to date(UTC) before which the aip should have been added to the system
     * @param tags
     * @param providerId
     * @param sessionOwner
     * @param session {@link String}
     * @param storages
     * @param pageable
     * @param assembler
     * @return page of aip metadata respecting the constraints
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Return a page of AIPs")
    public ResponseEntity<PagedResources<Resource<AIPEntity>>> searchAIPs(
            @RequestParam(name = REQUEST_PARAM_STATE, required = false) AIPState state,
            @RequestParam(name = REQUEST_PARAM_FROM,
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = REQUEST_PARAM_TO,
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = REQUEST_PARAM_TAGS, required = false) List<String> tags,
            @RequestParam(name = REQUEST_PARAM_PROVIDER_ID, required = false) String providerId,
            @RequestParam(name = REQUEST_PARAM_SESSION_OWNER, required = false) String sessionOwner,
            @RequestParam(name = REQUEST_PARAM_SESSION, required = false) String session,
            @RequestParam(name = REQUEST_PARAM_STORAGES, required = false) List<String> storages,
            @RequestParam(name = REQUEST_PARAM_CATEGORIES, required = false) List<String> categories,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AIPEntity> assembler) throws ModuleException {
        Page<AIPEntity> aips = aipService.search(state, from, to, tags, sessionOwner, session, providerId,
                storages, categories, pageable);
        return new ResponseEntity<>(toPagedResources(aips, assembler), HttpStatus.OK);
    }

    @RequestMapping(value = AIP_DOWNLOAD_PATH, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResourceAccess(description = "Download AIP as JSON file")
    public void downloadAIP(@RequestParam(required = false) String origin,
            @Valid @PathVariable(AIP_ID_PATH_PARAM) UniformResourceName aipId, HttpServletResponse response)
            throws ModuleException, IOException {

        LOGGER.debug("Downloading AIP file for entity \"{}\"", aipId.toString());

        try {
            aipService.downloadAIP(aipId, response);
        } catch (ModuleException e) {
            // Workaround to handle conversion of ServletErrorResponse in JSON format and
            // avoid using ContentType of file set before.
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            throw e;
        }
        response.getOutputStream().flush();
        response.setStatus(HttpStatus.OK.value());
    }

    @Override
    public Resource<AIPEntity> toResource(AIPEntity element, Object... extras) {
        Resource<AIPEntity> resource = resourceService.toResource(element);
        return resource;
    }

}