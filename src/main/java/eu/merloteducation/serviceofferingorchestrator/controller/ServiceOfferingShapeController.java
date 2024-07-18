/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.serviceofferingorchestrator.controller;

import eu.merloteducation.gxfscataloglibrary.service.GxfsWizardApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/shapes")
public class ServiceOfferingShapeController {

    private final GxfsWizardApiService gxfsWizardApiService;

    public ServiceOfferingShapeController(@Autowired GxfsWizardApiService gxfsWizardApiService) {
        this.gxfsWizardApiService = gxfsWizardApiService;
    }

    private static final String ECOSYSTEM_MERLOT = "merlot";
    private static final String ECOSYSTEM_GAIAX = "gx";


    /**
     * GET request for retrieving the Gaia-X service offering shape.
     *
     * @return catalog shape
     */
    @GetMapping("/gx/serviceoffering")
    public String getGxServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_GAIAX, "Serviceoffering.json");
    }

    /**
     * GET request for retrieving the MERLOT general service offering shape.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering")
    public String getMerlotServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotserviceoffering.json");
    }

    /**
     * GET request for retrieving the MERLOT Software as a Service service offering shape.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering/saas")
    public String getMerlotSaasServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotsaasserviceoffering.json");
    }

    /**
     * GET request for retrieving the MERLOT Data Delivery service offering shape.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering/datadelivery")
    public String getMerlotDataDeliveryServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotdatadeliveryserviceoffering.json");
    }

    /**
     * GET request for retrieving the MERLOT Coop Contract service offering shape.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering/coopcontract")
    public String getMerlotCoopContractServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotcoopcontractserviceoffering.json");
    }

}
