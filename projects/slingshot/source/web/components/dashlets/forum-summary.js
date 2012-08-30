/**
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Dashboard ForumSummary component.
 *
 * @namespace Alfresco.dashlet
 * @class Alfresco.dashlet.ForumSummary
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   var PREFERENCES_FORUM_SUMMARY_DASHLET = "org.alfresco.share.forum.summary.dashlet.";
   /**
    * Dashboard ForumSummary constructor.
    *
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.dashlet.ForumSummary} The new component instance
    * @constructor
    */
   Alfresco.dashlet.ForumSummary = function ForumSummary_constructor(htmlId)
   {
      Alfresco.dashlet.ForumSummary.superclass.constructor.call(this, "Alfresco.dashlet.ForumSummary", htmlId, ["container", "datasource", "datatable"]);
      // Services
      this.services.preferences = new Alfresco.service.Preferences();
      PREFERENCES_FORUM_SUMMARY_DASHLET = PREFERENCES_FORUM_SUMMARY_DASHLET + this.id;

      return this;
   };

   YAHOO.extend(Alfresco.dashlet.ForumSummary, Alfresco.component.Base,
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * Search root node
          *
          * @property searchRootNode
          * @type string
          * @default ""
          */
         searchRootNode: "",

         /**
          * Number of results to display in the forum summary dashlet
          *
          * @property resultSize
          * @type string
          * @default "10"
          */
         resultSize: "10",
         
         /**
          * Filters and options from the config file
          * 
          * @property filters
          * @type array
          * @default ""
          */
         filters: "",
         
         /**
          * Persistance for the selected options in the filter buttons
          * 
          * @property filterPreferences
          * @type array
          * @default ""
          */
         filterPreferences: []
      },

      /**
       * Fired by YUI when parent element is available for scripting
       *
       * @method onReady
       */
      onReady: function ForumSummary_onReady()
      {
         var me = this,
         id = this.id;

         // Load preferences
         this.services.preferences.request(PREFERENCES_FORUM_SUMMARY_DASHLET,
         {
            successCallback:
            {
               fn: function(p_oResponse)
               {
                  for each(var filter in me.options.filters)
                  {
                     var button = Alfresco.util.createYUIButton(this, filter.name, this.onFilterChanged,
                     {
                        type: "menu",
                        menu: filter.name + "-menu",
                        lazyloadmenu: false
                     });
                     var filterPrefName = PREFERENCES_FORUM_SUMMARY_DASHLET + "." + filter.name;
                     var selectedOption = Alfresco.util.findValueByDotNotation(p_oResponse.json, filterPrefName);
                     
                     if(selectedOption !== null)
                     {
                        for each(var option in filter.options)
                        {
                           if(option.value == selectedOption)
                           {
                              button.set("label", this.msg("filter." + filter.name + "." + option.label));
                              button.value = selectedOption;
                              break;
                           }
                        }
                     }
                     else
                     {
                        this.options.filterPreferences[filterPrefName] = filter.options[0].value;
                        var preferences = this.services.preferences;
                        preferences.set(filterPrefName, this.options.filterPreferences[filterPrefName]);
                        
                        button.set("label", this.msg("filter." + filter.name + "." + filter.options[0].label));
                        button.value = filter.options[0].value;
                     }

                     me.widgets[filter.name + "MenuButton"] = button;
                     me.options.filterPreferences[filterPrefName] = selectedOption;
                  }

                  me.doRequest();

                  // Display the toolbar now that we have selected the filter
                  Dom.removeClass(Selector.query(".toolbar div", id, true), "hidden");
               },
               scope: me
            }
         });
      },

      /**
       * Creates the data table and saves the preferences
       *
       * @method doRequest
       * @param {string} pref_term
       * @param {string} pref_resultSize
       */
      doRequest: function ForumSummary_doRequest(changedFilterName)
      {
         var me = this;
         
         me.widgets.alfrescoDataTable = new Alfresco.util.DataTable(
         {
            dataSource:
            {
               url: this.buildUrl(),
               config:
               {
                  responseSchema:
                  {
                     resultsList: 'items'
                  }
               }
            },
            dataTable:
            {
               container: this.id + "-filtered-topics",
               columnDefinitions:
               [
                  {key: "avatar", formatter: me.bind(me.buildThumbnail), width: 32},
                  {key: "topic", formatter: me.bind(me.buildDescription)}
               ],
               config:
               {
                  MSG_EMPTY: this.msg("no.result")
               }
            }
         });
      },

      /**
       * Called by the DataTable to build the 'topic' cell
       *
       * @method buildDescription
       * @param elCell {object}
       * @param oRecord {object}
       * @param oColumn {object}
       * @param oData {object|string}
       */
      buildDescription: function ForumSummary_buildDescription(elCell, oRecord, oColumn, oData)
      {
         var name = oRecord.getData("name"),
         	title = oRecord.getData("title"),
            author = oRecord.getData("author"),
            replies = oRecord.getData("totalReplyCount"),
            replyAuthor = oRecord.getData("lastReplyBy"),
            isUpdated = oRecord.getData("isUpdated"),
            createdDate =  Alfresco.util.relativeTime(oRecord.getData("createdOn")),
            updatedDate = Alfresco.util.relativeTime(oRecord.getData("updatedOn")),
            lastReplyDate = Alfresco.util.relativeTime(oRecord.getData("lastReplyOn")),
            site = oRecord.getData("site"),
            url = Alfresco.constants.URL_PAGECONTEXT + "site/" + site + "/discussions-topicview?topicId=" + name + "&listViewLinkBack=true";

         var cellContent = "<div class=\"node topic\">";
         cellContent += "<span class=\"nodeTitle\"><a href=\"" + url + "\">" + title + "</a>";
         if(isUpdated)
         {
            cellContent += "<span class=\"theme-color-2 nodeStatus\"> (" + this.msg("topicList.updated") + " " + updatedDate + ")</span>"; 
         }
         cellContent += "</span>";
         cellContent += "<div class=\"published\">";
         cellContent += "<span>" + this.msg("topicList.createdBy", this.getAuthorLink(author, "theme-color-1"), createdDate) + "</span>";
         if(replies > 0)
         {
            cellContent += "<br>";
            if(replies = 1)
            {
               cellContent += "<span class=\"nodeAttrLabel\">" + this.msg("topicList.replies.single") + " </span>";
            }
            else
            {
               cellContent += "<span class=\"nodeAttrLabel\">" + this.msg("topicList.replies.plural", replies) + " </span>";
            }
            cellContent += "<span class=\"nodeAttrLabel\">" + this.msg("topicList.lastReplyBy", this.getAuthorLink(replyAuthor, "theme-color-1"), lastReplyDate) + "</span>";
         }
         cellContent += "</div>";
         
         elCell.innerHTML = cellContent;
      },
      
      /**
       * Called by the DataTable to render the 'avatar' cell
       *
       * @method buildThumbnail
       * @param elCell {object}
       * @param oRecord {object}
       * @param oColumn {object}
       * @param oData {object|string}
       *
       */
      buildThumbnail: function SiteSearch_buildThumbnail(elCell, oRecord, oColumn, oData)
      {
         elCell.innerHTML = Alfresco.Share.userAvatar(oRecord.getData("author").username, 32);
      },
      
      /**
       * Builds the url for topic list
       *
       * @method buildUrl
       * @return {string} The url for the data table
       */
      buildUrl: function ForumSummary_buildUrl()
      {
         var parameters = "";
         
         for each(filter in this.options.filters)
         {
            var filterValue = this.options.filterPreferences[PREFERENCES_FORUM_SUMMARY_DASHLET + "." + filter.name];
            parameters += filter.name + "=" + filterValue + "&";
         }
         parameters += "resultSize=" + this.options.resultSize;
         
         var url = "";
         if(this.options.siteId.length > 0)
    	 {
        	 url = Alfresco.constants.PROXY_URI + "api/forum/site/{site}/discussions/posts/filtered?{parameters}";
    	 }
         else
    	 {
        	 url = Alfresco.constants.PROXY_URI + "api/forum/discussions/posts/filtered?{parameters}";
    	 }
         
         return YAHOO.lang.substitute(url,
         {
            site: this.options.siteId,
            parameters: parameters
         });
      },
     
      /**
       * Updates the filter value and refreshed the topic list when a drop-down menu is changed.
       *
       * @param {string} p_sType The event
       * @param {array} p_aArgs Event arguments
       */
      onFilterChanged: function ForumSummary_onFilterChanged(p_sType, p_aArgs)
      {
         var menuItem = p_aArgs[1];
         var filterName = p_aArgs[2];
         var button = this.widgets[filterName + "MenuButton"];

         if (menuItem !== null)
         {
            var filterPrefName = PREFERENCES_FORUM_SUMMARY_DASHLET + "." + filterName;
            this.options.filterPreferences[filterPrefName] = menuItem.value;
            var preferences = this.services.preferences;
            preferences.set(filterPrefName, menuItem.value);
            
            button.set("label", menuItem.element.textContent);
            button.value = menuItem.value;
            
            //Finally update the topics list
            this.doRequest();
         }
      },
      
      /**
       * Returns a link to the author's profile page
       *
       * @param {object} author the user object for the post author
       * @param {string} classString class string
       */
      getAuthorLink: function ForumSummary_getAuthorLink(author, classString)
      {
         var fullName = author.firstName + ((author.firstName !== "" && author.lastName !== "")? " " : "") + author.lastName;
         
         return Alfresco.util.userProfileLink(author.username, fullName, "class=\""+classString+"\"");
      }
   });
})();