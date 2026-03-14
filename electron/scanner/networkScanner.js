/**
 * 网络连接扫描器
 *
 * 通过调用系统命令（netstat / ss）获取当前活跃网络连接，
 * 匹配已知AI服务域名或IP，识别正在进行的AI服务通信。
 *
 * 注意：仅能识别当前时刻的活跃连接；历史连接需结合浏览器历史扫描。
 */

'use strict';

const { execSync } = require('child_process');
const { AI_SERVICES } = require('./aiServiceList');

/**
 * 获取系统当前活跃连接的外部主机名/IP列表。
 * @returns {string[]} 地址列表，如 ['chat.openai.com', '104.18.x.x', ...]
 */
function getActiveConnections() {
  const platform = process.platform;
  let output = '';

  try {
    if (platform === 'win32') {
      output = execSync('netstat -n 2>nul', { timeout: 5000 }).toString();
    } else if (platform === 'darwin') {
      output = execSync('netstat -an -p tcp 2>/dev/null', { timeout: 5000 }).toString();
    } else {
      // Linux
      output = execSync('ss -tun 2>/dev/null || netstat -tn 2>/dev/null', { timeout: 5000 }).toString();
    }
  } catch (e) {
    // 命令失败时返回空列表
    return [];
  }

  const addresses = new Set();
  const lines = output.split('\n');

  for (const line of lines) {
    // 提取外部地址（IPv4）
    const ipMatch = line.match(/(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)/g);
    if (ipMatch) {
      ipMatch.forEach(addr => {
        const [ip] = addr.split(':');
        if (ip && !isLocalAddress(ip)) {
          addresses.add(ip);
        }
      });
    }

    // 提取带主机名的地址（如 chat.openai.com:443）
    const hostMatch = line.match(/([a-zA-Z0-9][-a-zA-Z0-9.]+\.[a-zA-Z]{2,}):(\d+)/g);
    if (hostMatch) {
      hostMatch.forEach(addr => {
        const host = addr.split(':')[0];
        if (host) addresses.add(host);
      });
    }
  }

  return [...addresses];
}

function isLocalAddress(ip) {
  return ip === '127.0.0.1' || ip === '0.0.0.0' || ip.startsWith('192.168.') ||
    ip.startsWith('10.') || ip.startsWith('172.16.') || ip === '::1';
}

/**
 * 扫描网络连接，返回发现的AI服务列表。
 * @returns {{ service: object, matchedAddress: string }[]}
 */
function scanNetworkConnections() {
  const activeAddresses = getActiveConnections();
  const found = [];

  for (const service of AI_SERVICES) {
    for (const domain of service.domains) {
      const domainHost = domain.split(':')[0]; // 去除端口

      const matched = activeAddresses.find(addr =>
        addr === domainHost ||
        addr.endsWith('.' + domainHost) ||
        domainHost.endsWith('.' + addr)
      );

      if (matched) {
        found.push({
          name: service.name,
          domain,
          category: service.category,
          riskLevel: service.riskLevel,
          source: 'network',
          description: service.description,
          matchedAddress: matched,
          lastSeen: new Date().toISOString(),
        });
        break; // 每个服务只记录一次
      }
    }
  }

  return found;
}

module.exports = { scanNetworkConnections };
