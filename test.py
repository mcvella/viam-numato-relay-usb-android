import asyncio

from viam.robot.client import RobotClient
from viam.rpc.dial import Credentials, DialOptions
from viam.components.generic import Generic

async def connect():
    opts = RobotClient.Options.with_api_key( 
        api_key='***REMOVED***',
        api_key_id='***REMOVED***'
    )
    return await RobotClient.at_address('tablet1-main.fafsm877c6.viam.cloud', opts)

async def main():
    machine = await connect()

    print('Resources:')
    print(machine.resource_names)
    
    relay = Generic.from_robot(robot=machine, name="relay")

    res = await relay.do_command({"off": "0"})
    print(res)
    # Don't forget to close the machine when you're done!
    await machine.close()

if __name__ == '__main__':
    asyncio.run(main())
