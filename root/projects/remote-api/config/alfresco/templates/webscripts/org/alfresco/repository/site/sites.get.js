function main()
{
    // Get the filter parameters
    var nameFilter = args["nf"];
    var sitePreset = args["spf"];
    var sizeString = args["size"];
    
    if (nameFilter[0] !== '*') nameFilter = "*" + nameFilter;
    // Get the list of sites
    var sites = siteService.getSites(nameFilter, sitePreset, sizeString != null ? parseInt(sizeString) : -1);
    model.sites = sites;
    model.roles = (args["roles"] !== null ? args["roles"] : "managers");
}

main();