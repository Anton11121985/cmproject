SELECT
    res.id::text AS resId,
	res.id_type::text AS resIdtype,
	author.id::text as authorid,
    COALESCE(to_char(res.ctrldeadline, 'DD.MM.YYYY'), '') AS ctrldeadline,
    (SELECT string_agg(author_resp.v,', ')
        FROM (SELECT concat(author_resp.orig_shortname,
					(case when author_resp.id not in (SELECT executorcurr
													  FROM F_DP_ResltnBase_Execcurr Execcurr WHERE Execcurr.OWNER = res.id)
						then ' (' || orgname || ')' else '' end)) as v
					FROM F_DP_ResltnBase_ExecResp resp
          JOIN So_Beard author_resp ON author_resp.id = resp.ExecutorResp
          WHERE resp.owner = res.id
          ORDER BY resp.idx) AS author_resp
    ) AS ExecResp,
     COALESCE((SELECT
          string_agg(curr_exec_data.executor_name, ', ') AS author_curr
      FROM (
               SELECT
                   concat(sb_exec_curr.orig_shortname,
						 (case
						  	when sb_exec_curr.id not in (SELECT executorcurr FROM F_DP_ResltnBase_Execcurr Execcurr WHERE Execcurr.OWNER = res.id) then ' (' || orgname || ')'
						  	else ''
						 end)) as executor_name
               FROM f_dp_resltnbase_execsrted curr
               JOIN So_Beard sb_exec_curr on cmjunid like curr.executorsorted || '%'
               WHERE (sb_exec_curr.id NOT IN
                      (SELECT EXECUTORRESP FROM F_DP_ResltnBase_ExecResp ExecResp WHERE ExecResp.OWNER = res.id))
               AND curr.owner = res.id
               ORDER BY curr.idx
           ) AS curr_exec_data
     ), '') AS ExecCurr,
     COALESCE(resBase.resolution, '') as resolution,
     COALESCE(to_char(res.Date, 'DD.MM.YYYY'), '') AS resDate,
     author.orig_shortname AS authorName
FROM F_DP_Resolution res
    JOIN F_DP_ResltnBase resBase ON resBase.id = res.id
    JOIN So_Beard author ON author.id = resBase.author
	JOIN f_dp_rkkbase rkkbase ON rkkbase.id = res.hierroot
	JOIN ss_module module ON module.id = rkkbase.module
	JOIN SS_ModuleType moduletype ON moduletype.id = module.type
WHERE 
	res.HierParent = '$id'
	and res.hierparent_type = '$type'
	AND res.isproject = 0
    AND moduletype.Alias <> 'TempStorage' AND resBase.isdeleted = 0
    AND resBase.realdocisnew <> 1
ORDER BY resBase.created_date