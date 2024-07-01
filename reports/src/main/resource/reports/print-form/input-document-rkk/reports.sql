select 
COALESCE(repo.text, '') as text
,author.id as authorid
,author.orig_shortname || case when author.orig_shortname != realauthor.orig_shortname then ' (' || realauthor.orig_shortname || ')' else '' end as author
,COALESCE(to_char(repo.execdate, 'DD.MM.YYYY'), '') as execdate
,(select idx from f_dp_resltnbase_execsrted curr
  JOIN So_Beard sb_exec_curr on cmjunid like curr.executorsorted || '%'
  where curr.owner = repo.hierparent AND author.id = sb_exec_curr.id) as idx
from f_dp_report repo
JOIN f_dp_rkkbase rkkbase ON rkkbase.id = repo.hierroot
JOIN ss_module module ON module.id = rkkbase.module
JOIN SS_ModuleType moduletype ON moduletype.id = module.type
left JOIN So_Beard author ON author.id = repo.author
left JOIN So_Beard realauthor ON realauthor.id = repo.realauthor
where 
	repo.isdeleted = 0
	AND repo.hierparent = '$id'
	AND repo.hierparent_type = '$type'
	AND moduletype.Alias <> 'TempStorage' AND repo.isdeleted = 0
	AND repo.realdocisnew <> 1
	AND (
	$param = '-3'
	OR $param = '-1' AND author.id IN ($authorid)
	OR $param = '-2' AND author.id NOT IN ($authorid))
order by idx