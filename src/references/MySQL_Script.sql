drop database if exists filemanagersvc;
create database filemanagersvc;

DELETE FROM mysql.user WHERE user = 'filemanageruser';
grant all privileges on filemanagersvc.* to 'filemanageruser'@'%' identified by 'fmcu';

use filemanagersvc;
drop table if exists fileuser;
create table fileuser (
	id bigint not null auto_increment primary key,
    username varchar(80) not null
);
create unique index uq_fileuser on fileuser(username);

drop table if exists filelist;
create table filelist (
	id bigint not null auto_increment primary key,
    fileuser_id bigint not null,
    filename varchar(250) not null,
    ispublic varchar(1) not null default 'N',
    mimetype varchar(35) not null,
    date_updated TIMESTAMP default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
create unique index uq_filelist on filelist(fileuser_id, filename);
create index ix_filelist_file on filelist(filename);

drop table if exists filedata;
create table filedata (
	id bigint not null auto_increment primary key,
    filelist_id bigint not null,
    data longblob null not null
);
create unique index uq_filedata on filedata(filelist_id);

delimiter //
drop procedure if exists updateFile //
create procedure updateFile(IN iusername varchar(80), IN ifilename varchar(250), IN iispublic varchar(1),
	IN imimetype varchar(35), IN idata BLOB, OUT ouserid bigint, OUT ofileid bigint, OUT odataid bigint)
begin
	declare user_id bigint default 0;
    declare file_id bigint default 0;
    declare data_id bigint default 0;
    
    -- check and update user
    select id into user_id from fileuser where username = iusername;
    if (user_id = 0) then
		insert into fileuser(username) values (iusername);
        select LAST_INSERT_ID() into user_id;
    end if;
    
	-- check and update filelist
	select id into file_id from filelist where fileuser_id = user_id and filename = ifilename;

    -- if data is not null, add/update
    if (idata IS NOT null) then
		if (file_id = 0) then
			insert into filelist(fileuser_id, filename, ispublic, mimetype) values
				(user_id, ifilename, iispublic, imimetype);
			select LAST_INSERT_ID() into file_id;
		end if;
		
		-- check and store the filedata (delete old first)
		delete from filedata where filelist_id = file_id;
		insert into filedata (filelist_id, data) values (file_id, idata);
		select LAST_INSERT_ID() into data_id;
	else
		-- delete all info for the file
        delete from filelist where fileuser_id = user_id and filename = ifilename;
		delete from filedata where filelist_id = file_id;
    end if;
    
    -- return variables
    set ouserid = user_id;
    set ofileid = file_id;
    set odataid = data_id;
end //

drop view if exists vwFile //
create view vwFile as
select a.id as user_id, a.username, 
	b.id as file_id, b.filename, b.ispublic, b.mimetype, b.date_updated
    from fileuser a
    inner join filelist b on b.fileuser_id = a.id //

drop view if exists vwFileData //
create view vwFileData as
select a.id as user_id, a.username, 
	b.id as file_id, b.filename, b.ispublic, b.mimetype, b.date_updated,
	c.data
    from fileuser a
	inner join filelist b on b.fileuser_id = a.id
    inner join filedata c on c.filelist_id = b.id //
delimiter ;

select * from vwFile;
select * from vwFileData;

call updateFile("test", "test", "Y", "image/png", "tehis is a test", @oid, @fid, @did);