export const ROLE = {
  ADMIN: 'admin',
  MAINTAINER: 'maintainer',
  USER: 'user'
}

export const PERMISSIONS = {
  ADMIN: [
    'dashboard:view','users:manage','devices:manage','repair:all:view','repair:audit','repair:reject',
    'repair:assign','repair:reassign','repair:delay:approve','repair:close','repair:stats','repair:export',
    'repair:record:view','repair:record:manage','log:view','notice:publish','notice:manage','report:export'
  ],
  MAINTAINER: [
    'dashboard:view','devices:view','repair:assigned:view','repair:accept','repair:reject:receive',
    'repair:start','repair:progress:update','repair:photo:upload','repair:delay:apply',
    'repair:parts:apply','repair:finish','repair:record:view','repair:record:manage','notice:view'
  ],
  USER: [
    'dashboard:view','repair:create','repair:cancel','repair:my:view','repair:progress:view',
    'repair:confirm','repair:feedback','devices:view','notice:view'
  ]
}

export const ROUTE_ROLE_MAP = {
  '/users': [ROLE.ADMIN],
  '/devices': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER],
  '/repair-orders': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER],
  '/repair-records': [ROLE.ADMIN, ROLE.MAINTAINER],
  '/notices': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER],
  '/logs': [ROLE.ADMIN],
  '/profile': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER],
  '/': [ROLE.ADMIN, ROLE.MAINTAINER, ROLE.USER]
}

export const MENU_CONFIG = [
  { path: '/', label: '首页', perm: 'dashboard:view' },
  { path: '/users', label: '用户管理', perm: 'users:manage' },
  { path: '/devices', label: '设备管理', anyPerm: ['devices:manage', 'devices:view'] },
  { path: '/repair-orders', label: '报修工单', anyPerm: ['repair:all:view', 'repair:assigned:view', 'repair:my:view'] },
  { path: '/repair-records', label: '维修记录', perm: 'repair:record:view' },
  { path: '/notices', label: '公告管理', anyPerm: ['notice:manage', 'notice:view'] },
  { path: '/logs', label: '日志管理', perm: 'log:view' },
  { path: '/profile', label: '个人中心' }
]
