export const ROLE = {
  ADMIN: 'admin',
  MAINTAINER: 'maintainer',
  USER: 'user'
}

export const PERMISSIONS = {
  ADMIN: [
    'user:manage', 'user:query:employee-no', 'role:manage', 'device:manage', 'device:view',
    'repair:order:approve', 'repair:order:assign', 'repair:order:view:all', 'repair:record:view', 'repair:supervise',
    'notice:publish', 'notice:view', 'statistics:view', 'report:export', 'log:operation:view', 'log:business:view'
  ],
  MAINTAINER: [
    'device:view', 'repair:order:view:self', 'repair:order:accept', 'repair:order:reject',
    'repair:order:progress', 'repair:record:view', 'repair:record:write', 'repair:attachment:upload',
    'repair:expected-finish:update', 'repair:delay:apply', 'repair:parts:apply', 'notice:view'
  ],
  USER: [
    'device:view', 'repair:order:create', 'repair:order:view:self', 'repair:record:view',
    'repair:progress:track', 'repair:feedback:confirm', 'notice:view'
  ]
}

export const ROUTE_ROLE_MAP = {
  '/users': [ROLE.ADMIN],
  '/devices': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER],
  '/repair-orders': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER],
  '/repair-apply': [ROLE.USER],
  '/my-repairs': [ROLE.USER],
  '/maintainer-orders': [ROLE.MAINTAINER],
  '/repair-records': [ROLE.ADMIN, ROLE.MAINTAINER],
  '/notices': [ROLE.ADMIN],
  '/logs': [ROLE.ADMIN],
  '/profile': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER],
  '/': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER]
}

export const MENU_CONFIG = [
  { path: '/', label: '首页', anyPerm: ['statistics:view', 'repair:order:view:self', 'repair:order:view:all'] },
  { path: '/users', label: '用户管理', perm: 'user:manage' },
  { path: '/devices', label: '设备管理', anyPerm: ['device:manage', 'device:view'] },
  { path: '/repair-orders', label: '报修工单', anyPerm: ['repair:order:view:all', 'repair:order:view:self', 'repair:order:create'] },
  { path: '/repair-apply', label: '报修申请', anyPerm: ['repair:order:create'] },
  { path: '/my-repairs', label: '我的报修', anyPerm: ['repair:order:view:self', 'repair:progress:track', 'repair:feedback:confirm'] },
  { path: '/maintainer-orders', label: '我的待处理工单', anyPerm: ['repair:order:view:self', 'repair:order:progress'] },
  { path: '/repair-records', label: '维修记录', anyPerm: ['repair:record:view', 'repair:record:write'] },
  { path: '/notices', label: '公告管理', perm: 'notice:publish' },
  { path: '/logs', label: '日志管理', anyPerm: ['log:operation:view', 'log:business:view'] },
  { path: '/profile', label: '个人中心' }
]
