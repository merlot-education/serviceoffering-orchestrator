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
     * GET request for retrieving a specific MERLOT shapes for the catalog.
     *
     * @return catalog shape
     */
    @GetMapping("/gx/serviceoffering")
    public String getGxServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_GAIAX, "Serviceoffering.json");
    }

    /**
     * GET request for retrieving a specific MERLOT shapes for the catalog.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering")
    public String getMerlotServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotserviceoffering.json");
    }

    /**
     * GET request for retrieving a specific MERLOT shapes for the catalog.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering/saas")
    public String getMerlotSaasServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotsaasserviceoffering.json");
    }

    /**
     * GET request for retrieving a specific MERLOT shapes for the catalog.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering/datadelivery")
    public String getMerlotDataDeliveryServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotdatadeliveryserviceoffering.json");
    }

    /**
     * GET request for retrieving a specific MERLOT shapes for the catalog.
     *
     * @return catalog shape
     */
    @GetMapping("/merlot/serviceoffering/coopcontract")
    public String getMerlotCoopContractServiceOfferingShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotcoopcontractserviceoffering.json");
    }

}
