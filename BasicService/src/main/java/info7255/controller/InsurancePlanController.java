package info7255.controller;


import info7255.service.AuthorizeService;
import info7255.service.MessageQueueService;
import info7255.service.PlanService;
import info7255.validator.JsonValidator;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;


@RestController
@RequestMapping(path = "/")
public class InsurancePlanController {

    @Autowired
    JsonValidator validator;

    @Autowired
    PlanService planservice;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private MessageQueueService messageQueueService;


    @PostMapping(path ="/token", produces = "application/json")
    public ResponseEntity<Object> createToken(@RequestHeader("authorization") String idToken, @Valid @RequestBody(required = false) String medicalPlan) throws Exception {
        if (medicalPlan == null || medicalPlan.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("Error", "Body is Empty. Kindly provide the JSON").toString());
        }

        return ResponseEntity.ok().body(idToken);
    }


    @PostMapping(path ="/plan", produces = "application/json")
    public ResponseEntity<Object> createPlan(@RequestHeader("authorization") String idToken, @RequestHeader HttpHeaders headers, @Valid @RequestBody(required = false) String medicalPlan) throws Exception {
        if (medicalPlan == null || medicalPlan.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("Error", "Body is Empty. Kindly provide the JSON").toString());
        }

        //Authorize
        if (!authorizeService.authorize(idToken.substring(7))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is invalid");


        JSONObject plan = new JSONObject(medicalPlan);
        try{
            validator.validateJson(plan);
        }catch(ValidationException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("Error",ex.getErrorMessage()).toString());
        }

        //create a key for plan: objecyType + objectID
        String key = plan.get("objectType").toString() + "_" + plan.get("objectId").toString();
        //check if plan exists
        if(planservice.isKeyExists(key)){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new JSONObject().put("Message", "Plan already exist").toString());
        }

        //save the plan if not exist
        String newEtag = planservice.addPlanETag(plan, plan.get("objectId").toString());
        String res = "{ObjectId: " + plan.get("objectId") + ", ObjectType: " + plan.get("objectType") + "}";
        return ResponseEntity.ok().eTag(newEtag).body(new JSONObject(res).toString());//TODO: test
    }

    @PatchMapping(path = "/plan/{objectId}", produces = "application/json")
    public ResponseEntity<Object> patchPlan(@RequestHeader("authorization") String idToken, @RequestHeader HttpHeaders headers, @Valid @RequestBody String medicalPlan, @PathVariable String objectId) throws IOException {

        //Authorize
        if (!authorizeService.authorize(idToken.substring(7))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is invalid");

        JSONObject plan = new JSONObject(medicalPlan);
        String key = "plan_" + objectId;
        if (!planservice.isKeyExists(key)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONObject().put("Message", "ObjectId does not exist").toString());
        }

        //return status 412 if a mid-air update occurs (e.g. etag/header is different from etag/in-processing)
        String actualEtag = planservice.getEtag(objectId, "eTag");
        String eTag = headers.getFirst("If-Match");
        if (eTag != null && !eTag.equals(actualEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag).build();
        }

        //update if the plan already created
        String newEtag = planservice.addPlanETag(plan, plan.get("objectId").toString());
        return ResponseEntity.ok().eTag(newEtag).body(new JSONObject().put("Message ", "Updated successfully").toString());
    }


    @GetMapping(path = "/{type}/{objectId}",produces = "application/json ")
    public ResponseEntity<Object> getPlan(@RequestHeader("authorization") String idToken, @RequestHeader HttpHeaders headers, @PathVariable String objectId,@PathVariable String type) throws JSONException, Exception {

        String key = type + "_" + objectId;
        if (!planservice.isKeyExists(key)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("Message", "ObjectId does not exist").toString());
        }

        //Authorize
        if (!authorizeService.authorize(idToken.substring(7))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is invalid");

        String actualEtag = null;
        if (type.equals("plan")) {
            actualEtag = planservice.getEtag(objectId, "eTag");
            String eTag = headers.getFirst("if-none-match");
            //if not updated -> 304
            if (actualEtag.equals(eTag)){
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(actualEtag).build();
            }
        }

        Map<String, Object> plan = planservice.getPlan(key);
        if (type.equals("plan")) {
            return ResponseEntity.ok().eTag(actualEtag).body(new JSONObject(plan).toString());
        }

        return ResponseEntity.ok().body(new JSONObject(plan).toString());
    }

    @PutMapping(path = "/plan/{objectId}", produces = "application/json")
    public ResponseEntity<Object> updatePlan(@RequestHeader("authorization") String idToken, @RequestHeader HttpHeaders headers, @Valid @RequestBody String medicalPlan, @PathVariable String objectId) throws IOException {

        //Authorize
        if (!authorizeService.authorize(idToken.substring(7))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is invalid");

        JSONObject plan = new JSONObject(medicalPlan);
        try {
            validator.validateJson(plan);
        } catch (ValidationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("Validation Error", ex.getMessage()).toString());
        }

        String key = "plan_" + objectId;
        //check if the target for update exist
        if (!planservice.isKeyExists(key)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONObject().put("Message", "ObjectId does not exist").toString());
        }

        // return status 412 if a mid-air update occurs (e.g. etag/header is different from etag/in-processing)
        String actualEtag = planservice.getEtag(objectId, "eTag");
        String eTag = headers.getFirst("If-Match");
        if (eTag != null && !eTag.equals(actualEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag).build();
        }

        planservice.deletePlan("plan" + "_" + objectId);
        String newEtag = planservice.addPlanETag(plan, plan.get("objectId").toString());
        return ResponseEntity.ok().eTag(newEtag).body(new JSONObject().put("Message: ", "Updated successfully").toString());
    }


    @DeleteMapping("/plan/{objectId}")
    public ResponseEntity<Object> getPlan(@RequestHeader("authorization") String idToken, @PathVariable String objectId){

        if (!planservice.isKeyExists("plan"+ "_" + objectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("Message", "ObjectId does not exist").toString());
        }

        if (!authorizeService.authorize(idToken.substring(7))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is invalid");

        planservice.deletePlan("plan" + "_" + objectId);

        messageQueueService.addToMessageQueue(objectId, true);
        return ResponseEntity.noContent().build();

    }

}
