package com.appsmith.server.domains;

import com.appsmith.external.models.BaseDomain;
import com.appsmith.external.models.PluginType;
import com.appsmith.external.views.Views;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Transient;

import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Deprecated
public class Action extends BaseDomain {

    @JsonView(Views.Public.class)
    String name;

    // @JsonView(Views.Public.class)
    // Datasource datasource;

    // Organizations migrated to workspaces, kept the field as depricated to support the old migration
    @Deprecated
    @JsonView(Views.Public.class)
    String organizationId;

    @JsonView(Views.Public.class)
    String workspaceId;

    @JsonView(Views.Public.class)
    String pageId;

    @JsonView(Views.Public.class)
    String collectionId;

    // @JsonView(Views.Public.class)
    // ActionConfiguration actionConfiguration;

    @JsonView(Views.Public.class)
    PluginType pluginType;

    @JsonView(Views.Public.class)
    Boolean executeOnLoad;

    /*
     * This is a list of fields specified by the client to signify which fields have dynamic bindings in them.
     * TODO: The server can use this field to simplify our Mustache substitutions in the future
     */
    // @JsonView(Views.Public.class)
    // List<Property> dynamicBindingPathList;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonView(Views.Public.class)
    Boolean isValid;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonView(Views.Public.class)
    Set<String> invalids;

    // This is a list of keys that the client whose values the client needs to send during action execution.
    // These are the Mustache keys that the server will replace before invoking the API
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonView(Views.Public.class)
    Set<String> jsonPathKeys;

    @JsonView(Views.Internal.class)
    String cacheResponse;

    @JsonView(Views.Public.class)
    String templateId; // If action is created via a template, store the id here.

    @JsonView(Views.Public.class)
    String providerId; // If action is created via a template, store the template's provider id here.

    @Transient
    @JsonView(Views.Public.class)
    String pluginId;

    @JsonView(Views.Internal.class)
    Boolean userSetOnLoad = false;

    @JsonView(Views.Public.class)
    Boolean confirmBeforeExecute = false;

    // @JsonView(Views.Public.class)
    // Documentation documentation;

    /**
     * If the Datasource is null, create one and set the autoGenerated flag to true. This is required because spring-data
     * cannot add the createdAt and updatedAt properties for null embedded objects. At this juncture, we couldn't find
     * a way to disable the auditing for nested objects.
     *
     * @return
     */
    // @JsonView(Views.Public.class)
    // public Datasource getDatasource() {
    //     if (this.datasource == null) {
    //         this.datasource = new Datasource();
    //         this.datasource.setIsAutoGenerated(true);
    //     }
    //     return datasource;
    // }
}
